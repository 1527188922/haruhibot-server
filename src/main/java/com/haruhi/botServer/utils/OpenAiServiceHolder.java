package com.haruhi.botServer.utils;

import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.config.service.ConfigApplier;
import com.haruhi.botServer.config.service.ConfigChange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;
import java.util.List;

/**
 * OpenAiService 持有者
 * <p>
 * 因为持有的是"已经建好的客户端（含连接池与线程池）"，配置变更后必须重建，
 * 所以实现 {@link ConfigApplier} 订阅 ds.api.* 三项配置：改完配置立刻重建，下次调用即用新配置。
 */
@Slf4j
@Component
public class OpenAiServiceHolder implements ConfigApplier {

    private static volatile OpenAiService openAiService;

    public static OpenAiService getOpenAiService() {
        if (openAiService != null) {
            return openAiService;
        }

        synchronized (OpenAiServiceHolder.class) {
            if (openAiService != null) {
                return openAiService;
            }
            openAiService = create();
            return openAiService;
        }
    }

    /**
     * 释放已有实例，下次调用时按当前配置重建
     */
    public static synchronized void refresh() {
        if (openAiService != null) {
            try {
                openAiService.shutdownExecutor();
            } catch (Exception ignored) {
            }
            openAiService = null;
        }
    }

    @Override
    public List<ConfigKey> keys() {
        return List.of(ConfigKey.DEEP_SEEK_API_KEY, ConfigKey.DEEP_SEEK_API_BASE_URL, ConfigKey.DEEP_SEEK_API_TIMEOUT);
    }

    @Override
    public void onConfigChange(ConfigChange change) {
        log.info("DeepSeek配置变更({}),重建OpenAiService", change.keyName());
        refresh();
    }

    private static OpenAiService create() {
        try {
            String token = Configs.getStr(ConfigKey.DEEP_SEEK_API_KEY, "");
            int timeoutInt = Configs.getInt(ConfigKey.DEEP_SEEK_API_TIMEOUT, 30);
            String baseUrl = Configs.getStr(ConfigKey.DEEP_SEEK_API_BASE_URL, "");
            return create(baseUrl, token, timeoutInt);
        } catch (Exception e) {
            log.error("创建OpenAiService异常", e);
            return null;
        }
    }

    public static OpenAiService create(String baseUrl, String apiKey, int timeoutInSeconds) {
        if (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(apiKey)) {
            return null;
        }
        Duration timeout = Duration.ofSeconds(timeoutInSeconds);

//        OpenAiApi openAiApi = OpenAiService.buildApi(token, timeout);
        ObjectMapper mapper = OpenAiService.defaultObjectMapper();
        OkHttpClient client = OpenAiService.defaultClient(apiKey, timeout);
//        Retrofit retrofit = OpenAiService.defaultRetrofit(client, mapper);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        OpenAiApi openAiApi = retrofit.create(OpenAiApi.class);
        return new OpenAiService(openAiApi);
    }
}
