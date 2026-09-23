package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.HaruhiBotServer;
import com.haruhi.botserver.shared.model.BaseResp;
import com.haruhi.botserver.features.jmcomic.client.model.jmcomic.Album;
import com.haruhi.botserver.shared.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@ActiveProfiles("dev")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = HaruhiBotServer.class)
class JmcomicServiceTest {
    @Autowired
    private JmcomicService jmcomicService;

    @Test
    void executeWithJmLockBlocksDifferentAlbumsWhenParallelDisabled() throws Exception {
        TestJmcomicService service = new TestJmcomicService(false);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<BaseResp<String>> first = CompletableFuture.supplyAsync(() ->
                service.executeWithJmLock("100", "下载漫画", () -> {
                    entered.countDown();
                    release.await(3, TimeUnit.SECONDS);
                    return BaseResp.success("ok");
                }));

        assertTrue(entered.await(3, TimeUnit.SECONDS));
        BaseResp<String> second = service.executeWithJmLock("200", "生成zip", () -> BaseResp.success("unexpected"));
        release.countDown();

        assertFalse(second.isSuccess());
        assertEquals("已有JM漫画任务正在执行，请稍后再试", second.getMsg());
        assertTrue(first.get(3, TimeUnit.SECONDS).isSuccess());
    }

    @Test
    void executeWithJmLockAllowsDifferentAlbumsWhenParallelEnabled() throws Exception {
        TestJmcomicService service = new TestJmcomicService(true);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<BaseResp<String>> first = CompletableFuture.supplyAsync(() ->
                service.executeWithJmLock("100", "下载漫画", () -> {
                    entered.countDown();
                    release.await(3, TimeUnit.SECONDS);
                    return BaseResp.success("ok");
                }));

        assertTrue(entered.await(3, TimeUnit.SECONDS));
        BaseResp<String> second = service.executeWithJmLock("200", "生成zip", () -> BaseResp.success("ok"));
        release.countDown();

        assertTrue(second.isSuccess());
        assertTrue(first.get(3, TimeUnit.SECONDS).isSuccess());
    }

    @Test
    void executeWithJmLockBlocksSameAlbumWhenParallelEnabled() throws Exception {
        TestJmcomicService service = new TestJmcomicService(true);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        CompletableFuture<BaseResp<String>> first = CompletableFuture.supplyAsync(() ->
                service.executeWithJmLock("100", "下载漫画", () -> {
                    entered.countDown();
                    release.await(3, TimeUnit.SECONDS);
                    return BaseResp.success("ok");
                }));

        assertTrue(entered.await(3, TimeUnit.SECONDS));
        BaseResp<String> second = service.executeWithJmLock("100", "生成pdf", () -> BaseResp.success("unexpected"));
        release.countDown();

        assertFalse(second.isSuccess());
        assertEquals("【JM100】正在执行下载漫画任务，请稍后再试", second.getMsg());
        assertTrue(first.get(3, TimeUnit.SECONDS).isSuccess());
    }
    @Test
    void testDownload(){

        try {
//            BaseResp<Album> albumBaseResp = jmcomicService.requestAlbum("452699");
//            BaseResp<String> stringBaseResp = jmcomicService.downloadAlbum(albumBaseResp.getData());
            jmcomicService.downloadImage("https://cdn-msp2.jmapiproxy2.cc/media/photos/749671/00001.webp",
                    new File(FileUtil.getAppTempDir() + File.separator + "00001.webp.tmp"));
            Thread.sleep(Duration.ofMinutes(10).toMillis());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestJmcomicService extends JmcomicService {
        private final boolean parallel;

        private TestJmcomicService(boolean parallel) {
            this.parallel = parallel;
        }

        @Override
        protected boolean isJmOperationParallel() {
            return parallel;
        }
    }
}
