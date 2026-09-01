package com.haruhi.botServer.service;

import com.haruhi.botServer.dto.BaseResp;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JmcomicServiceTest {

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
