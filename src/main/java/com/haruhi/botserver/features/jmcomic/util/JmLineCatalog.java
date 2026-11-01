package com.haruhi.botserver.features.jmcomic.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * JM 线路清单（硬编码快照），移植自 Android 工程
 * {@code app/src/main/java/com/haruhi/jmcomic/core/protocol/LineCatalog.kt}。
 *
 * <p>这里同时是「配置管理」页下拉候选项与「测速」的**唯一数据来源**：
 * <ul>
 *     <li>{@link #apiLines()}：漫画详情 / 搜索 / 章节清单走的接口域名（{@code jm.api_domain}）</li>
 *     <li>{@link #imageLines()}：章节图片 CDN 域名（{@code jm.image_domain}）</li>
 * </ul>
 *
 * <p>清单是本地副本，**不每次启动去拉官方那份加密清单**：官方新增线路时，改这个文件再发版即可。
 *
 * <p>两个清单的默认项都排第一（就是工程当前在用的那两个域名），并带「默认线路」标记；
 * 「默认值」按钮回落到它们。清单里没有的域名（老用户手填过的）不会被丢掉：
 * 下拉允许自定义值，界面上照样能选中，测速时也会被单独测一次。
 *
 * <p>去重：两个清单构造时都按 host {@code distinct} 过一遍，重复线路不会出现两行。
 */
public final class JmLineCatalog {

    /** 默认接口线路：工程当前在用的那条（{@code jm.api_domain} 的默认值）。 */
    public static final String DEFAULT_API_HOST = "www.cdnbea.net";

    /** 默认图片线路：工程当前在用的那条（{@code jm.image_domain} 的默认值）。 */
    public static final String DEFAULT_IMAGE_HOST = "cdn-msp2.jmapiproxy2.cc";

    /** 默认接口线路在列表里显示的名字（它不在官方清单里，没有"线路N"编号）。 */
    private static final String DEFAULT_API_LABEL = "默认线路";

    /** 默认图片线路在列表里显示的名字。 */
    private static final String DEFAULT_IMAGE_LABEL = "默认线路";

    /**
     * 官方线路清单接口返回的线路（{@code jm3_Server} 的 {@code [域名, 线路名]}，名字改成简体）。
     *
     * <p>这份清单里**不含** {@link #DEFAULT_API_HOST}——那是本工程一直在用的线路，
     * 单独插到列表最前面。
     */
    private static final List<Line> LISTED_API_LINES = List.of(
            new Line("www.cdnhjk.net", "线路 1", true),
            new Line("www.cdngwc.cc", "线路 2", true),
            new Line("www.cdngwc.net", "线路 3", true),
            new Line("www.cdngwc.club", "线路 4", true),
            new Line("www.cdnutc.me", "线路 5", true)
    );

    /**
     * 实测收集到的图片线路（{@code /setting} 的 {@code img_host}，去掉 scheme、按域名去重）。
     *
     * <p>每个接口线路的 {@code app_img_shunt=1..4} 返回的图片 CDN 是**随机轮换**的，
     * 这 12 条是那几轮请求里真正出现过的全部取值；用户能不能连取决于自己的网络，
     * 所以照原样保留（连不上的会自动测速失败）。
     */
    private static final List<Line> OBSERVED_IMAGE_LINES = List.of(
            new Line("cdn-msp.jmapiproxy1.cc", "图源 1", true),
            new Line("cdn-msp.jmapiproxy3.cc", "图源 2", true),
            new Line("cdn-msp.jmdanjonproxy.vip", "图源 3", true),
            new Line("cdn-msp.jmdanjonproxy.xyz", "图源 4", true),
            new Line("cdn-msp12.jmdanjonproxy.xyz", "图源 5", true),
            new Line("cdn-msp2.jmapiproxy1.cc", "图源 6", true),
            new Line("cdn-msp2.jmapiproxy3.cc", "图源 7", true),
            new Line("cdn-msp2.jmdanjonproxy.vip", "图源 8", true),
            new Line("cdn-msp3.jmapiproxy1.cc", "图源 9", true),
            new Line("cdn-msp3.jmapiproxy3.cc", "图源 10", true),
            new Line("cdn-msp3.jmdanjonproxy.vip", "图源 11", true),
            new Line("tencent.jmdanjonproxy.xyz", "图源 12", true)
    );

    /** 接口线路清单（默认项排第一）。 */
    private static final List<Line> API_LINES = buildLines(DEFAULT_API_HOST, DEFAULT_API_LABEL, LISTED_API_LINES);

    /** 图片线路清单（默认项排第一）。 */
    private static final List<Line> IMAGE_LINES = buildLines(DEFAULT_IMAGE_HOST, DEFAULT_IMAGE_LABEL, OBSERVED_IMAGE_LINES);

    private JmLineCatalog() {
    }

    /** 接口线路清单（不可变，默认项在第一）。 */
    public static List<Line> apiLines() {
        return API_LINES;
    }

    /** 图片线路清单（不可变，默认项在第一）。 */
    public static List<Line> imageLines() {
        return IMAGE_LINES;
    }

    /**
     * 「配置管理」下拉候选项：{@code host:显示名} 逗号分隔（ControlMeta 的格式）。
     * <p>
     * 显示名里带上域名，用户只看下拉也能知道选的是哪条线路。
     */
    public static String apiControlOptions() {
        return toControlOptions(API_LINES);
    }

    /** 图片线路的「配置管理」下拉候选项，格式同 {@link #apiControlOptions()}。 */
    public static String imageControlOptions() {
        return toControlOptions(IMAGE_LINES);
    }

    /** 接口线路里 {@code host} 对应的那一条；清单里没有时返回 {@code builtIn=false} 的临时线路。 */
    public static Line apiLineOf(String host) {
        return lineOf(API_LINES, normalizeHost(host));
    }

    /** 图片线路里 {@code host} 对应的那一条；清单里没有时返回 {@code builtIn=false} 的临时线路。 */
    public static Line imageLineOf(String host) {
        return lineOf(IMAGE_LINES, normalizeHost(host));
    }

    public static boolean isDefaultApi(String host) {
        return DEFAULT_API_HOST.equals(normalizeHost(host));
    }

    public static boolean isDefaultImage(String host) {
        return DEFAULT_IMAGE_HOST.equals(normalizeHost(host));
    }

    /**
     * 域名规整：去掉首尾空白、{@code scheme://}、路径与查询串，转小写。
     *
     * <p>转小写是因为 {@code /setting} 的 {@code base_url} 与用户手填的历史值大小写不统一，
     * 不归一会让"同一条线路"在列表里对不上。
     */
    public static String normalizeHost(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        int schemeIndex = value.indexOf("://");
        if (schemeIndex >= 0) {
            value = value.substring(schemeIndex + 3);
        }
        int cut = value.length();
        for (char c : new char[]{'/', '?', '#'}) {
            int index = value.indexOf(c);
            if (index >= 0 && index < cut) {
                cut = index;
            }
        }
        return value.substring(0, cut).trim().toLowerCase(Locale.ROOT);
    }

    private static Line lineOf(List<Line> lines, String host) {
        if (host == null || host.isEmpty()) {
            return lines.get(0);
        }
        for (Line line : lines) {
            if (line.host().equals(host)) {
                return line;
            }
        }
        return new Line(host, host, false);
    }

    private static List<Line> buildLines(String defaultHost, String defaultLabel, List<Line> listed) {
        Set<String> seen = new LinkedHashSet<>();
        List<Line> lines = new ArrayList<>(listed.size() + 1);
        lines.add(new Line(defaultHost, defaultLabel, true));
        seen.add(defaultHost);
        for (Line line : listed) {
            if (seen.add(line.host())) {
                lines.add(line);
            }
        }
        return List.copyOf(lines);
    }

    private static String toControlOptions(List<Line> lines) {
        StringBuilder builder = new StringBuilder();
        for (Line line : lines) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            // 显示名里带域名；括号用全角，避免与 ControlMeta 的 "值:显示名" 分隔符混淆
            builder.append(line.host()).append(':').append(line.label())
                    .append('（').append(line.host()).append('）');
        }
        return builder.toString();
    }

    /**
     * 一条线路。
     *
     * @param host    域名，不含 scheme 与路径（{@code www.cdngwc.cc}）
     * @param label   给用户看的名字（{@code 线路 2}）
     * @param builtIn 是否在硬编码清单里。{@code false} 只会出现在"用户手填了清单之外的域名"时，
     *                那时把它当成当前线路原样显示，不改用户配置
     */
    public record Line(String host, String label, boolean builtIn) {

        /** 下拉里的显示名。 */
        public String displayName() {
            return label + "（" + host + "）";
        }
    }

    /**
     * 线路种类。
     *
     * <p>{@link #code()} 会写进「配置管理」的控件元数据（{@code control.probe}），
     * 前端测速时再原样回传，所以取值要稳定。
     */
    public enum Kind {
        /** 接口线路（{@code jm.api_domain}）。 */
        API("JM_API"),
        /** 图片线路（{@code jm.image_domain}）。 */
        IMAGE("JM_IMAGE"),
        ;

        private final String code;

        Kind(String code) {
            this.code = code;
        }

        /** 前端回传用的稳定编码。 */
        public String code() {
            return code;
        }

        /** 清单：接口线路 / 图片线路。 */
        public List<Line> lines() {
            return this == API ? apiLines() : imageLines();
        }

        /** 编码 → 种类；认不出时返回 null（调用方给出参数错误）。 */
        public static Kind fromCode(String code) {
            if (code == null) {
                return null;
            }
            String value = code.trim();
            for (Kind kind : values()) {
                if (kind.code.equalsIgnoreCase(value) || kind.name().equalsIgnoreCase(value)) {
                    return kind;
                }
            }
            return null;
        }
    }
}
