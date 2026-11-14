package com.haruhi.botserver.features.ai.client.deepseek;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.ConfigApplier;
import com.haruhi.botserver.configuration.service.ConfigChange;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * DeepSeek Chat Completions / Balance 的轻量 HTTP 客户端。
 * <p>
 * 不复用 theokanning 的 OpenAiService：0.18.2 的 ChatCompletionRequest 不支持
 * response_format、thinking、reasoning_effort、user_id 等参数。
 */
@Component
@Slf4j
public class DeepSeekClient implements ConfigApplier {

    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private volatile OkHttpClient httpClient;

    @Override
    public Collection<ConfigKey> keys() {
        return List.of(ConfigKey.DEEP_SEEK_API_KEY, ConfigKey.DEEP_SEEK_API_BASE_URL, ConfigKey.DEEP_SEEK_API_TIMEOUT);
    }

    @Override
    public void onConfigChange(ConfigChange change) {
        log.info("DeepSeek配置变更({}),重建聊天HTTP客户端", change.keyName());
        refresh();
    }

    public synchronized void refresh() {
        OkHttpClient old = httpClient;
        httpClient = null;
        if (old != null) {
            old.dispatcher().executorService().shutdown();
            old.connectionPool().evictAll();
        }
    }

    /**
     * JSON 模式聊天，带重试。
     *
     * @param userId  已哈希的业务会话标识；为空时使用匿名值
     * @param maxTokenOverride 大于0时覆盖配置中的 max_tokens
     */
    public DeepSeekChatResponse chat(List<DeepSeekMessage> messages, String userId, Integer maxTokenOverride) {
        DeepSeekApiException last = null;
        int retry = AiChatSettings.retry();
        for (int attempt = 0; attempt <= retry; attempt++) {
            try {
                return doChat(messages, userId, maxTokenOverride);
            } catch (DeepSeekApiException e) {
                last = e;
                if (!e.retryable() || attempt >= retry) {
                    break;
                }
                long sleep = 500L * (1L << attempt) + ThreadLocalRandom.current().nextLong(250);
                log.warn("DeepSeek请求失败({}),{}ms后重试 {}/{}", e.getMessage(), sleep, attempt + 1, retry);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last == null ? new DeepSeekApiException("DeepSeek请求失败", null) : last;
    }

    private DeepSeekChatResponse doChat(List<DeepSeekMessage> messages, String userId, Integer maxTokenOverride) {
        String apiKey = Configs.getStr(ConfigKey.DEEP_SEEK_API_KEY, "");
        if (StringUtils.isBlank(apiKey)) {
            throw new DeepSeekApiException(401, "authentication", "missing_api_key", "DeepSeek API Key 未配置");
        }

        int maxTokens = maxTokenOverride != null && maxTokenOverride > 0
                ? maxTokenOverride : AiChatSettings.maxTokens();

        JSONObject body = new JSONObject(true);
        body.put("model", AiChatSettings.model());
        JSONArray messageArray = new JSONArray(messages.size());
        for (DeepSeekMessage message : messages) {
            JSONObject item = new JSONObject(true);
            item.put("role", message.getRole());
            item.put("content", message.getContent() == null ? "" : message.getContent());
            if (StringUtils.isNotBlank(message.getName())) {
                item.put("name", message.getName());
            }
            messageArray.add(item);
        }
        body.put("messages", messageArray);
        body.put("max_tokens", maxTokens);
        body.put("stream", false);
        body.put("user_id", userId == null ? "anonymous" : userId);

        if (AiChatSettings.thinkingEnabled()) {
            JSONObject thinking = new JSONObject(true);
            thinking.put("type", "enabled");
            body.put("thinking", thinking);
            body.put("reasoning_effort", AiChatSettings.reasoningEffort());
        } else {
            JSONObject thinking = new JSONObject(true);
            thinking.put("type", "disabled");
            body.put("thinking", thinking);
            body.put("temperature", AiChatSettings.temperature());
        }
        JSONObject responseFormat = new JSONObject(true);
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);

        Request request = new Request.Builder()
                .url(endpoint("/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), JSON_MEDIA))
                .build();

        try (Response response = client().newCall(request).execute()) {
            String responseBody = readBody(response);
            if (!response.isSuccessful()) {
                throw parseError(response.code(), responseBody);
            }
            return parseChat(responseBody);
        } catch (DeepSeekApiException e) {
            throw e;
        } catch (IOException e) {
            throw new DeepSeekApiException("DeepSeek网络异常: " + e.getMessage(), e);
        }
    }

    /**
     * 官方余额接口：GET /user/balance
     */
    public DeepSeekBalanceResponse getBalance() {
        String apiKey = Configs.getStr(ConfigKey.DEEP_SEEK_API_KEY, "");
        if (StringUtils.isBlank(apiKey)) {
            throw new DeepSeekApiException(401, "authentication", "missing_api_key", "DeepSeek API Key 未配置");
        }
        Request request = new Request.Builder()
                .url(balanceEndpoint())
                .header("Authorization", "Bearer " + apiKey)
                .get()
                .build();
        try (Response response = client().newCall(request).execute()) {
            String body = readBody(response);
            if (!response.isSuccessful()) {
                throw parseError(response.code(), body);
            }
            return parseBalance(body);
        } catch (DeepSeekApiException e) {
            throw e;
        } catch (IOException e) {
            throw new DeepSeekApiException("DeepSeek余额接口异常: " + e.getMessage(), e);
        }
    }

    private DeepSeekChatResponse parseChat(String body) {
        JSONObject root;
        try {
            root = JSONObject.parseObject(body);
        } catch (Exception e) {
            throw new DeepSeekApiException(200, "parse", "invalid_json", "DeepSeek响应不是合法JSON");
        }
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new DeepSeekApiException(200, "parse", "empty_choices", "DeepSeek响应没有choices");
        }
        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        DeepSeekChatResponse result = new DeepSeekChatResponse();
        result.setId(root.getString("id"));
        result.setModel(root.getString("model"));
        result.setFinishReason(choice.getString("finish_reason"));
        result.setRaw(body);
        if (message != null) {
            result.setContent(message.getString("content"));
            result.setReasoningContent(message.getString("reasoning_content"));
        }
        JSONObject usage = root.getJSONObject("usage");
        if (usage != null) {
            DeepSeekUsage u = new DeepSeekUsage();
            u.setPromptTokens(usage.getIntValue("prompt_tokens"));
            u.setCompletionTokens(usage.getIntValue("completion_tokens"));
            u.setTotalTokens(usage.getIntValue("total_tokens"));
            u.setPromptCacheHitTokens(usage.getIntValue("prompt_cache_hit_tokens"));
            u.setPromptCacheMissTokens(usage.getIntValue("prompt_cache_miss_tokens"));
            result.setUsage(u);
        }
        return result;
    }

    private DeepSeekBalanceResponse parseBalance(String body) {
        JSONObject root = JSONObject.parseObject(body);
        DeepSeekBalanceResponse result = new DeepSeekBalanceResponse();
        result.setAvailable(root.getBooleanValue("is_available"));
        JSONArray infos = root.getJSONArray("balance_infos");
        if (infos != null) {
            for (int i = 0; i < infos.size(); i++) {
                JSONObject info = infos.getJSONObject(i);
                DeepSeekBalanceResponse.BalanceInfo item = new DeepSeekBalanceResponse.BalanceInfo();
                item.setCurrency(info.getString("currency"));
                item.setTotalBalance(info.getString("total_balance"));
                item.setGrantedBalance(info.getString("granted_balance"));
                item.setToppedUpBalance(info.getString("topped_up_balance"));
                result.getBalances().add(item);
            }
        }
        return result;
    }

    private DeepSeekApiException parseError(int status, String body) {
        String type = null;
        String code = null;
        String message = "HTTP " + status;
        try {
            JSONObject root = JSONObject.parseObject(body);
            JSONObject error = root == null ? null : root.getJSONObject("error");
            if (error != null) {
                type = error.getString("type");
                code = error.getString("code");
                message = error.getString("message");
            }
        } catch (Exception ignored) {
        }
        return new DeepSeekApiException(status, type, code, message);
    }

    private String readBody(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null) {
            return "";
        }
        String text = body.string();
        // 非流式请求等待期间服务端可能持续返回空行
        return text == null ? "" : text.trim();
    }

    private OkHttpClient client() {
        OkHttpClient local = httpClient;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (httpClient == null) {
                int timeout = Math.max(10, Configs.getInt(ConfigKey.DEEP_SEEK_API_TIMEOUT, 30));
                httpClient = new OkHttpClient.Builder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .readTimeout(Duration.ofSeconds(timeout))
                        .writeTimeout(Duration.ofSeconds(20))
                        .callTimeout(Duration.ofSeconds(timeout + 15L))
                        .build();
            }
            return httpClient;
        }
    }

    private String endpoint(String path) {
        return normalizeBase() + path;
    }

    private String balanceEndpoint() {
        String base = normalizeBase();
        if (base.endsWith("/v1")) {
            base = base.substring(0, base.length() - 3);
        }
        return base + "/user/balance";
    }

    private String normalizeBase() {
        String base = Configs.getStr(ConfigKey.DEEP_SEEK_API_BASE_URL, "https://api.deepseek.com");
        if (StringUtils.isBlank(base)) {
            base = "https://api.deepseek.com";
        }
        base = base.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    /**
     * 不把原始 QQ 传给 DeepSeek；用固定前缀 + SHA-256 前 16 位做业务隔离标识。
     */
    public static String hashUserKey(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((raw == null ? "anonymous" : raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i] & 0xff));
            }
            return "hbot-" + sb;
        } catch (Exception e) {
            return "hbot-anonymous";
        }
    }

    /**
     * 供测试/调试查看最终请求体。
     */
    public static String buildRequestBodyForTest(List<DeepSeekMessage> messages, String userId, int maxTokens) {
        JSONObject body = new JSONObject(true);
        body.put("model", AiChatSettings.model());
        JSONArray array = new JSONArray();
        for (DeepSeekMessage message : messages) {
            JSONObject item = new JSONObject(true);
            item.put("role", message.getRole());
            item.put("content", message.getContent());
            array.add(item);
        }
        body.put("messages", array);
        body.put("max_tokens", maxTokens);
        body.put("stream", false);
        body.put("user_id", userId);
        JSONObject format = new JSONObject();
        format.put("type", "json_object");
        body.put("response_format", format);
        return body.toJSONString();
    }

    /**
     * 仅用于把多币种余额格式化成一行文本，避免命令层重复拼装。
     */
    public static List<String> formatBalance(DeepSeekBalanceResponse balance) {
        List<String> lines = new ArrayList<>();
        if (balance == null) {
            return lines;
        }
        for (DeepSeekBalanceResponse.BalanceInfo info : balance.getBalances()) {
            lines.add(info.getCurrency() + " " + info.getTotalBalance()
                    + "（赠金 " + info.getGrantedBalance() + " / 充值 " + info.getToppedUpBalance() + "）");
        }
        return lines;
    }
}
