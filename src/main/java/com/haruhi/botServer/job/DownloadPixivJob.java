package com.haruhi.botServer.job;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.entity.PixivSqlite;
import com.haruhi.botServer.job.schedule.AbstractJob;
import com.haruhi.botServer.service.PixivSqliteService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "job.downloadPixiv.enable",havingValue = "1")
public class DownloadPixivJob extends AbstractJob {

    @Value("${job.downloadPixiv.cron}")
    private String cron;

    @Override
    public String cronExpression() {
        return cron;
    }

    @Autowired
    private PixivSqliteService pixivService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Map<String,Object> param = new HashMap<>(2);
        param.put("num",20);
        param.put("r18",0);

        Map<String,Object> paramR18 = new HashMap<>(2);
        paramR18.put("num",20);
        paramR18.put("r18",1);

        int taskSize = 2;
        int r18TaskSize = 1;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()){
            List<DownloadTask> downloadTasks = new ArrayList<>();
            for (int i = 0; i < taskSize; i++) {
                DownloadTask downloadTask = new DownloadTask(param);
                downloadTasks.add(downloadTask);
            }
            for (int i = 0; i < r18TaskSize; i++) {
                DownloadTask downloadTask = new DownloadTask(paramR18);
                downloadTasks.add(downloadTask);
            }

            List<CompletableFuture<LoliconPixResp>> futures = downloadTasks.stream()
                    .map(callable -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return callable.call();
                        } catch (Exception e) {
                            log.error("下载pixiv异常",e);
                            return null;
                        }
                    }, executor)).toList();

            CompletableFuture<List<LoliconPixResp>> allResults = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            ).thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList())
            );
            List<LoliconPixResp> results = allResults.join();
            if (CollectionUtils.isEmpty(results)) {
               return;
            }
            List<LoliconPixResp> respList = results.stream().filter(Objects::nonNull).toList();
            respList.forEach(resp -> {
                List<LoliconPixResp.PixivItem> data = resp.getData();
                if(CollectionUtils.isEmpty(data)){
                    log.error("response.getData() = null");
                    return;
                }
                int total = 0;
                for (LoliconPixResp.PixivItem pixivItem : data) {
                    long count = pixivService.count(new LambdaQueryWrapper<PixivSqlite>()
                            .eq(PixivSqlite::getImgUrl,pixivItem.getUrls().getOriginal()));
                    if(count == 0){
                        PixivSqlite pixivSqlite = new PixivSqlite();
                        pixivSqlite.setPid(pixivItem.getPid());
                        pixivSqlite.setTitle(pixivItem.getTitle());
                        pixivSqlite.setWidth(pixivItem.getWidth());
                        pixivSqlite.setHeight(pixivItem.getHeight());
                        pixivSqlite.setImgUrl(pixivItem.getUrls().getOriginal());
                        pixivSqlite.setUid(pixivItem.getUid());
                        pixivSqlite.setAuthor(pixivItem.getAuthor());
                        pixivSqlite.setIsR18(pixivItem.getR18() ? 1 : 0);
                        pixivSqlite.setTags(StringUtils.join(pixivItem.getTags(),","));
                        if(pixivService.save(pixivSqlite)){
                            total++;
                        }
                    }
                }
                log.info("本次pixiv下载{}条",total);
            });
        }
    }

    @AllArgsConstructor
    private static class DownloadTask implements Callable<LoliconPixResp>{
        private final Map<String,Object> param;

        @Override
        public LoliconPixResp call() throws Exception {
            String s = HttpUtil.urlWithForm(ThirdPartyURL.LOLICON, param, StandardCharsets.UTF_8, false);
            HttpRequest httpRequest = HttpUtil.createGet(s).timeout(12 * 1000);
            try (HttpResponse response = httpRequest.execute()){
                if (!response.isOk()) {
                    log.error("response.getStatus() = {}",response.getStatus());
                    return null;
                }
                String body = response.body();
                if(StringUtils.isBlank(body)){
                    log.error("response = null");
                    return null;
                }
                return JSONObject.parseObject(body, LoliconPixResp.class);
            }
        }
    }

    @Data
    public static class LoliconPixResp implements Serializable {
        private String error;
        private List<PixivItem> data;
        @Data
        public static class PixivItem implements Serializable {
            private String pid;
            private String p;
            private String uid;
            private String title;
            private String author;
            private Boolean r18;
            private Integer width;
            private Integer height;
            private List<String> tags;
            private String ext;
            private Long uploadDate;
            private Url urls;
            @Data
            public static class Url implements Serializable {
                private String original;
            }
        }
    }
}
