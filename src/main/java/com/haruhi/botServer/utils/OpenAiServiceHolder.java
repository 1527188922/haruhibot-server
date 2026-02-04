package com.haruhi.botServer.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;

@Slf4j
public class OpenAiServiceHolder {

    private static OpenAiService openAiService;

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
     *
     * @param mode 1系统启动刷新 2接口刷新 3bot命令刷新
     */
    public synchronized static void refresh(int mode){
        if (mode == 1) {
            return;
        }
        if (openAiService != null) {
            try {
                openAiService.shutdownExecutor();
            } catch (Exception ignored) {}
            openAiService = null;
        }
    }

    private static OpenAiService create(){
        try {
            DictionarySqliteService bean = ApplicationContextProvider.getBean(DictionarySqliteService.class);
            String token = bean.getInCache(DictionaryEnum.DEEP_SEEK_API_KEY.getKey(), "");
            int timeoutInt = bean.getInt(DictionaryEnum.DEEP_SEEK_API_TIMEOUT.getKey(), 30);
            String baseUrl = bean.getInCache(DictionaryEnum.DEEP_SEEK_API_BASE_URL.getKey(), "");
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
