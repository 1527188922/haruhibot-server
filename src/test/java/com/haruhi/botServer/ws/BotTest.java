package com.haruhi.botServer.ws;

import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.constant.QqClientActionEnum;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.service.DictionarySqliteService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.lang.reflect.Field;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BotTest {

    private static final String UPLOAD_FILE_PARALLEL_KEY = "bot.upload_file.parallel";

    @Test
    void wrapsSessionWithConcurrentDecorator() throws Exception {
        Bot bot = new Bot(1L, new TestWebSocketSession());

        assertInstanceOf(ConcurrentWebSocketSessionDecorator.class, getSession(bot));
    }

    @Test
    void queuedUploadCallbacksRunOneAtATimeInSubmissionOrder() throws Exception {
        List<String> original = setUploadFileParallel("false");
        TrackingUploadBot bot = new TrackingUploadBot();
        try {
            CountDownLatch callbacks = new CountDownLatch(2);

            bot.uploadPrivateFile(100L, "first.txt", "first.txt", 1000, response -> callbacks.countDown());
            assertTrue(bot.firstStarted.await(1, TimeUnit.SECONDS));

            bot.uploadGroupFile(200L, "second.txt", "second.txt", null, 1000, response -> callbacks.countDown());
            assertFalse(bot.secondStarted.await(100, TimeUnit.MILLISECONDS));

            bot.releaseFirst.countDown();

            assertTrue(callbacks.await(1, TimeUnit.SECONDS));
            assertEquals(List.of("first.txt", "second.txt"), bot.files);
            assertEquals(1, bot.maxActive.get());
        } finally {
            bot.close();
            restoreUploadFileParallel(original);
        }
    }

    @Test
    void parallelUploadCallbacksUseVirtualThreadsAndCanOverlap() throws Exception {
        List<String> original = setUploadFileParallel("true");
        TrackingUploadBot bot = new TrackingUploadBot();
        try {
            CountDownLatch callbacks = new CountDownLatch(2);

            bot.uploadPrivateFile(100L, "first.txt", "first.txt", 1000, response -> callbacks.countDown());
            assertTrue(bot.firstStarted.await(1, TimeUnit.SECONDS));

            bot.uploadPrivateFile(100L, "second.txt", "second.txt", 1000, response -> callbacks.countDown());
            assertTrue(bot.secondStarted.await(1, TimeUnit.SECONDS));

            bot.releaseFirst.countDown();

            assertTrue(callbacks.await(1, TimeUnit.SECONDS));
            assertEquals(2, bot.maxActive.get());
            assertTrue(bot.virtualThreadFlags.stream().allMatch(Boolean::booleanValue));
        } finally {
            bot.close();
            restoreUploadFileParallel(original);
        }
    }

    private static List<String> setUploadFileParallel(String value) {
        List<String> original = DictionarySqliteService.CACHE.get(UPLOAD_FILE_PARALLEL_KEY);
        DictionarySqliteService.CACHE.put(UPLOAD_FILE_PARALLEL_KEY, new ArrayList<>(List.of(value)));
        return original == null ? null : new ArrayList<>(original);
    }

    private static void restoreUploadFileParallel(List<String> original) {
        if (original == null) {
            DictionarySqliteService.CACHE.remove(UPLOAD_FILE_PARALLEL_KEY);
            return;
        }
        DictionarySqliteService.CACHE.put(UPLOAD_FILE_PARALLEL_KEY, original);
    }

    private WebSocketSession getSession(Bot bot) throws Exception {
        Field session = Bot.class.getDeclaredField("session");
        session.setAccessible(true);
        return (WebSocketSession) session.get(bot);
    }

    private static class TrackingUploadBot extends Bot {
        private final CountDownLatch firstStarted = new CountDownLatch(1);
        private final CountDownLatch secondStarted = new CountDownLatch(1);
        private final CountDownLatch releaseFirst = new CountDownLatch(1);
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();
        private final AtomicInteger calls = new AtomicInteger();
        private final List<String> files = new CopyOnWriteArrayList<>();
        private final List<Boolean> virtualThreadFlags = new CopyOnWriteArrayList<>();

        private TrackingUploadBot() {
            super(1L, new TestWebSocketSession());
        }

        @Override
        public <T, R> SyncResponse<R> sendSyncRequest(QqClientActionEnum action, T params, long timeout,
                                                       TypeReference<SyncResponse<R>> typeReference) {
            int callIndex = calls.incrementAndGet();
            int currentActive = active.incrementAndGet();
            maxActive.updateAndGet(currentMax -> Math.max(currentMax, currentActive));
            try {
                virtualThreadFlags.add(Thread.currentThread().isVirtual());
                files.add(String.valueOf(((Map<?, ?>) params).get("file")));
                if (callIndex == 1) {
                    firstStarted.countDown();
                    assertTrue(releaseFirst.await(1, TimeUnit.SECONDS));
                } else {
                    secondStarted.countDown();
                }
                return (SyncResponse<R>) new SyncResponse<>(0, "ok", null, null, null, "ok", null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return SyncResponse.failed();
            } finally {
                active.decrementAndGet();
            }
        }
    }

    private static class TestWebSocketSession implements WebSocketSession {

        @Override
        public String getId() {
            return "session-1";
        }

        @Override
        public URI getUri() {
            return URI.create("ws://localhost/ws");
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return HttpHeaders.EMPTY;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 0;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 0;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return Collections.emptyList();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) throws IOException {
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void close(org.springframework.web.socket.CloseStatus status) throws IOException {
        }
    }
}
