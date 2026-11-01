package com.haruhi.botserver.features.jmcomic.service;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.features.jmcomic.util.JmLineCatalog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * JM 线路测速（配置管理页「测速」按钮的后端）。
 *
 * <p>做法对齐 Android 工程 {@code feature/api/ApiLineProbe.kt} 与 {@code ImageLineProbe.kt}：
 * <ul>
 *     <li><b>接口线路</b>：{@code GET https://{host}/setting?app_img_shunt=1&lang=CN&t={ts}}，
 *         带 {@code token} / {@code tokenparam} 签名（签名只依赖时间戳，不需要登录），
 *         量**首字节耗时**（拿到响应头那一刻）；响应必须像 JM 信封（有非空的 {@code data}），
 *         否则按"返回内容异常"处理（被劫持/广告页也会返回 200）。</li>
 *     <li><b>图片线路</b>：{@code GET https://{host}/media/users/nopic-.gif?v=0}，
 *         带 UA、不带签名，量**整张探针图收完**的耗时（对应官方 App 的 {@code img.onload}）；
 *         404 时换官方兜底图 {@code /media/users/nopic-Male.gif} 再量一次。</li>
 * </ul>
 *
 * <p>档位（阈值照官方 {@code speedTestUtil.ts}）：绿 &lt;600ms、橙 600～1200ms、红 ≥1200ms，
 * 连不上/超时/内容不对记 {@code FAILED}。
 *
 * <p>固定超时（连接 5s、读 8s），不跟随配置里的请求超时：测速要的是可比的横截面，
 * 跟随用户设置会把"慢但能用"的线路误判成失败。
 */
@Slf4j
@Service
public class JmLineProbeService {

    /** 绿灯上限（毫秒），低于它算畅通。 */
    public static final long SMOOTH_UPPER_MS = 600L;
    /** 橙灯上限（毫秒），达到它算很慢。 */
    public static final long NORMAL_UPPER_MS = 1200L;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";
    private static final String APP_TOKEN_SECRET = "18comicAPP";
    private static final String APP_VERSION = "2.0.13";

    /** 接口线路探针：官方测速用的就是带 {@code app_img_shunt} 的 {@code /setting}。 */
    private static final String API_PROBE_PATH = "/setting?app_img_shunt=1&lang=CN";
    /** 图片线路探针图（1～4KB 的占位头像，一定存在、不需要登录）。 */
    private static final String IMAGE_PROBE_PATH = "/media/users/nopic-.gif?v=0";
    /** 探针图 404 时的兜底图（官方文档里 {@code photo} 为空时的头像兜底图）。 */
    private static final String IMAGE_FALLBACK_PROBE_PATH = "/media/users/nopic-Male.gif";

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 8_000;
    /** 单条线路的整体上限：读超时 + 连接超时之外再兜一层，防止个别线路把整轮测速拖死。 */
    private static final long PROBE_TIMEOUT_MS = 12_000L;
    /** 逐条探测的并发数：线路本身不多，8 条并发就能让一整轮在 15 秒内结束。 */
    private static final int MAX_PARALLEL = 8;
    /** 读取响应体的上限，防止被劫持后灌一个超大页面把内存吃掉。 */
    private static final int MAX_BODY_BYTES = 512 * 1024;

    /**
     * 测一轮线路。
     *
     * @param kind       线路种类（接口 / 图片）
     * @param extraHosts 额外要测的域名（用户手填、清单里没有的；可为空）
     * @return 每条线路的结果，顺序 = 清单顺序 + 额外域名（按传入顺序）
     */
    public List<JmLineProbeResult> probe(JmLineCatalog.Kind kind, List<String> extraHosts) {
        List<JmLineCatalog.Line> targets = new ArrayList<>(kind.lines());
        Set<String> known = new LinkedHashSet<>();
        for (JmLineCatalog.Line line : targets) {
            known.add(line.host());
        }
        if (extraHosts != null) {
            for (String raw : extraHosts) {
                String host = JmLineCatalog.normalizeHost(raw);
                if (StringUtils.isBlank(host) || !known.add(host)) {
                    continue;
                }
                targets.add(new JmLineCatalog.Line(host, host, false));
            }
        }
        if (targets.isEmpty()) {
            return List.of();
        }

        int threads = Math.min(targets.size(), MAX_PARALLEL);
        ExecutorService pool = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "jm-line-probe");
            // 测速是页面上的旁路操作，不要因为它阻止进程退出
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<Future<JmLineProbeResult>> futures = new ArrayList<>(targets.size());
            for (JmLineCatalog.Line line : targets) {
                futures.add(pool.submit(() -> kind == JmLineCatalog.Kind.API
                        ? probeApiLine(line)
                        : probeImageLine(line)));
            }
            List<JmLineProbeResult> results = new ArrayList<>(targets.size());
            for (int i = 0; i < futures.size(); i++) {
                JmLineCatalog.Line line = targets.get(i);
                try {
                    results.add(futures.get(i).get(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS));
                } catch (Exception e) {
                    // 单条失败不影响其它线路的结果
                    if (!(e instanceof InterruptedException)) {
                        log.debug("JM线路测速失败 kind={} host={} err={}", kind, line.host(),
                                e.getClass().getSimpleName());
                    }
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    futures.get(i).cancel(true);
                    results.add(JmLineProbeResult.failed(line, "测速超时"));
                }
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * 接口线路探针：量到响应头（首字节）为止，再校验响应像不像 JM 信封。
     */
    private JmLineProbeResult probeApiLine(JmLineCatalog.Line line) {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        String url = "https://" + line.host() + API_PROBE_PATH + "&t=" + ts;
        HttpURLConnection connection = null;
        long started = System.nanoTime();
        try {
            connection = open(url);
            connection.setRequestProperty("token", DigestUtils.md5Hex(ts + APP_TOKEN_SECRET));
            connection.setRequestProperty("tokenparam", ts + "," + APP_VERSION);
            connection.setRequestProperty("user-agent", USER_AGENT);
            int status = connection.getResponseCode();
            long elapsed = elapsedMs(started);
            if (status < 200 || status >= 300) {
                return JmLineProbeResult.failed(line, "服务器返回 " + status, elapsed);
            }
            String body = readBody(connection);
            if (!looksLikeJmEnvelope(body)) {
                return JmLineProbeResult.failed(line, "返回内容异常", elapsed);
            }
            return JmLineProbeResult.ok(line, elapsed);
        } catch (IOException e) {
            return JmLineProbeResult.failed(line, userMessage(e), null);
        } finally {
            disconnect(connection);
        }
    }

    /**
     * 图片线路探针：量"整张探针图收完"的耗时；404 时换兜底图再量一次。
     */
    private JmLineProbeResult probeImageLine(JmLineCatalog.Line line) {
        ProbeAttempt primary = probeImageUrl(line, IMAGE_PROBE_PATH);
        // 404 = 这个镜像上没有这张探针图，不等于"这条线路不能用"，换兜底图再量一次
        if (primary.httpStatus != null && primary.httpStatus == 404) {
            return probeImageUrl(line, IMAGE_FALLBACK_PROBE_PATH).result;
        }
        return primary.result;
    }

    private ProbeAttempt probeImageUrl(JmLineCatalog.Line line, String path) {
        String url = "https://" + line.host() + path;
        HttpURLConnection connection = null;
        long started = System.nanoTime();
        try {
            connection = open(url);
            connection.setRequestProperty("user-agent", USER_AGENT);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return new ProbeAttempt(
                        JmLineProbeResult.failed(line, "服务器返回 " + status, elapsedMs(started)),
                        status);
            }
            byte[] body = readBytes(connection);
            long elapsed = elapsedMs(started);
            if (body.length == 0 || looksLikeHtml(connection.getContentType(), body)) {
                return new ProbeAttempt(JmLineProbeResult.failed(line, "返回内容异常", elapsed), status);
            }
            return new ProbeAttempt(JmLineProbeResult.ok(line, elapsed), status);
        } catch (IOException e) {
            return new ProbeAttempt(JmLineProbeResult.failed(line, userMessage(e), null), null);
        } finally {
            disconnect(connection);
        }
    }

    private HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("accept", "*/*");
        return connection;
    }

    private String readBody(HttpURLConnection connection) throws IOException {
        byte[] bytes = readBytes(connection);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readBytes(HttpURLConnection connection) throws IOException {
        try (InputStream in = connection.getInputStream()) {
            byte[] buffer = new byte[8192];
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (out.size() >= MAX_BODY_BYTES) {
                    break;
                }
            }
            return out.toByteArray();
        }
    }

    /**
     * 响应像不像 JM 的加密信封：{@code {"code":200,"data":"..."}}。
     *
     * <p>只认"有非空 data"，不解密：测速不需要内容，能拿到 data 就说明这条线路签得过、解析得出。
     */
    static boolean looksLikeJmEnvelope(String body) {
        if (StringUtils.isBlank(body)) {
            return false;
        }
        try {
            JSONObject json = JSONObject.parseObject(body);
            return json != null && StringUtils.isNotBlank(json.getString("data"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 响应像不像一个 HTML 页面。
     *
     * <p>被运营商/门户页劫持时是 {@code 200 + HTML}，只看状态码会把它当成"畅通"；
     * 真图片的第一个有效字节不可能是 {@code '<'}。
     */
    static boolean looksLikeHtml(String contentType, byte[] body) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
            return true;
        }
        for (byte b : body) {
            if (b == ' ' || b == '\n' || b == '\r' || b == '\t') {
                continue;
            }
            return b == '<';
        }
        return false;
    }

    /**
     * 毫秒 → 档位。抽成静态方法便于单测直接打表验证阈值。
     */
    public static String levelOf(long latencyMs) {
        if (latencyMs < SMOOTH_UPPER_MS) {
            return JmLineProbeResult.LEVEL_SMOOTH;
        }
        if (latencyMs < NORMAL_UPPER_MS) {
            return JmLineProbeResult.LEVEL_NORMAL;
        }
        return JmLineProbeResult.LEVEL_LAGGY;
    }

    private static String userMessage(IOException e) {
        if (e instanceof java.net.SocketTimeoutException) {
            return "连接超时";
        }
        String message = e.getMessage();
        return StringUtils.isBlank(message) ? "网络异常" : ("网络异常：" + message);
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }

    private static void disconnect(HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception ignored) {
                // 断开失败不影响测速结果
            }
        }
    }

    /**
     * 一次探测的中间结果。
     *
     * @param result     交给界面显示的结论
     * @param httpStatus 服务端状态码；网络层就失败（超时/连不上）时为 null。
     *                   只用来判断"要不要换备用探针图再试一次"
     */
    private record ProbeAttempt(JmLineProbeResult result, Integer httpStatus) {
    }

    /**
     * 一条线路的测速结果。
     *
     * @param host      域名
     * @param label     显示名（清单里的名字；额外域名就是域名本身）
     * @param latencyMs 耗时（毫秒）：接口线路是首字节，图片线路是整张探针图收完；失败时可为 null
     * @param level     档位：SMOOTH / NORMAL / LAGGY / FAILED
     * @param message   失败原因（中文，可直接展示）；成功时为 null
     * @param builtIn   是否在硬编码清单里
     */
    public record JmLineProbeResult(String host, String label, Long latencyMs, String level,
                                    String message, boolean builtIn) {

        public static final String LEVEL_SMOOTH = "SMOOTH";
        public static final String LEVEL_NORMAL = "NORMAL";
        public static final String LEVEL_LAGGY = "LAGGY";
        public static final String LEVEL_FAILED = "FAILED";

        static JmLineProbeResult ok(JmLineCatalog.Line line, long latencyMs) {
            return new JmLineProbeResult(line.host(), line.label(), latencyMs,
                    levelOf(latencyMs), null, line.builtIn());
        }

        static JmLineProbeResult failed(JmLineCatalog.Line line, String message) {
            return failed(line, message, null);
        }

        static JmLineProbeResult failed(JmLineCatalog.Line line, String message, Long latencyMs) {
            return new JmLineProbeResult(line.host(), line.label(), latencyMs,
                    LEVEL_FAILED, message, line.builtIn());
        }
    }
}
