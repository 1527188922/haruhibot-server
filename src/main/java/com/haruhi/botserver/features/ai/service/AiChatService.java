package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekChatResponse;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekClient;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekMessage;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekUsage;
import com.haruhi.botserver.features.ai.model.AiAmbientMessage;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiPrompt;
import com.haruhi.botserver.features.ai.model.AiReply;
import com.haruhi.botserver.features.ai.model.AiReplyMessage;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.model.AiTurn;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.features.ai.support.prompt.PromptAssembler;
import com.haruhi.botserver.features.ai.support.prompt.PromptTemplates;
import com.haruhi.botserver.features.ai.support.protocol.AiReplyParser;
import com.haruhi.botserver.features.ai.support.protocol.AiReplySanitizer;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 对话编排：会话队列、合并等待、模型调用、JSON 协议、工具循环、发送。
 */
@Service
@Slf4j
public class AiChatService {

    private final Map<AiSessionKey, SessionQueue> queues = new ConcurrentHashMap<>();

    private final AiSessionService sessionService;
    private final AiContextService contextService;
    private final PromptAssembler promptAssembler;
    private final DeepSeekClient deepSeekClient;
    private final AiSendService sendService;
    private final AiToolService toolService;

    private final AtomicInteger active = new AtomicInteger();
    private final AtomicLong dailyTokens = new AtomicLong();
    private final AtomicLong tokenDay = new AtomicLong(LocalDate.now().toEpochDay());

    public AiChatService(AiSessionService sessionService,
                         AiContextService contextService,
                         PromptAssembler promptAssembler,
                         DeepSeekClient deepSeekClient,
                         AiSendService sendService,
                         AiToolService toolService) {
        this.sessionService = sessionService;
        this.contextService = contextService;
        this.promptAssembler = promptAssembler;
        this.deepSeekClient = deepSeekClient;
        this.sendService = sendService;
        this.toolService = toolService;
    }

    public void enqueue(AiSessionKey key, AiTurn turn) {
        if (key == null || turn == null || turn.getIncoming() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (turn.getNotBefore() <= 0 || turn.getNotBefore() < now) {
            turn.setNotBefore(now);
        }
        if (turn.getDeadline() <= 0) {
            turn.setDeadline(turn.getNotBefore() + AiChatSettings.debounceMaxWaitMillis());
        }

        SessionQueue queue = queues.computeIfAbsent(key, ignored -> new SessionQueue());
        boolean submit;
        synchronized (queue) {
            AiTurn last = queue.pending.peekLast();
            if (last != null && last.userId() == turn.userId()) {
                merge(last, turn);
            } else {
                queue.pending.addLast(turn);
                if (queue.pending.size() > 3) {
                    // 太多待处理：丢弃最旧的一轮，避免无限堆积（不跨用户合并）
                    AiTurn dropped = queue.pending.pollFirst();
                    if (dropped != null) {
                        log.warn("AI会话队列积压，丢弃最旧一轮: {}", key.cacheKey());
                    }
                }
            }
            submit = !queue.running.get();
            if (submit) {
                queue.running.set(true);
            }
        }
        if (submit) {
            ThreadPoolUtil.getHandleCommandPool().execute(() -> work(key, queue));
        }
    }

    private void merge(AiTurn target, AiTurn incoming) {
        if (target == null || incoming == null) {
            return;
        }
        AiIncoming a = target.getIncoming();
        AiIncoming b = incoming.getIncoming();
        if (b.getText() != null && !b.getText().isBlank()) {
            a.appendText(b.getText());
            a.setAtOnly(false);
        }
        a.getMessageIds().addAll(b.getMessageIds());
        if (b.isHasImage()) {
            a.setHasImage(true);
        }
        target.setDirect(target.isDirect() || incoming.isDirect());
        long now = System.currentTimeMillis();
        long debounce = AiChatSettings.debounceWaitMillis();
        long cap = target.getDeadline() > 0 ? target.getDeadline() : now + AiChatSettings.debounceMaxWaitMillis();
        target.setNotBefore(Math.min(cap, Math.max(target.getNotBefore(), now + debounce)));
    }

    private void work(AiSessionKey key, SessionQueue queue) {
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                AiTurn turn;
                synchronized (queue) {
                    turn = queue.pending.peekFirst();
                    if (turn == null) {
                        queue.running.set(false);
                        if (!queue.pending.isEmpty() && queue.running.compareAndSet(false, true)) {
                            continue;
                        }
                        return;
                    }
                }
                long now = System.currentTimeMillis();
                if (turn.getDeadline() > 0 && now >= turn.getDeadline()) {
                    turn.setNotBefore(now);
                }
                long wait = turn.getNotBefore() - now;
                if (wait > 0) {
                    sleep(Math.min(wait, 200));
                    continue;
                }
                synchronized (queue) {
                    queue.pending.pollFirst();
                }
                try {
                    process(key, turn);
                } catch (Exception e) {
                    log.error("处理AI会话轮次异常: {}", key.cacheKey(), e);
                }
            }
        } catch (Exception e) {
            log.error("AI会话worker异常: {}", key.cacheKey(), e);
        } finally {
            synchronized (queue) {
                queue.running.set(false);
                if (!queue.pending.isEmpty() && queue.running.compareAndSet(false, true)) {
                    ThreadPoolUtil.getHandleCommandPool().execute(() -> work(key, queue));
                }
            }
        }
    }

    private void process(AiSessionKey key, AiTurn turn) {
        if (isOverDailyLimit()) {
            log.warn("AI每日token限额已到，跳过本轮: {}", key.cacheKey());
            return;
        }
        AiSessionState state = sessionService.load(key);
        if (state.paused() || turn.getBot() == null) {
            return;
        }
        long generation = state.generation();
        AiIncoming incoming = turn.getIncoming();
        List<AiAmbientMessage> ambient = key.group()
                ? contextService.recent(key, AiChatSettings.ambientSize(), incoming.getMessageIds())
                : List.of();
        AiPrompt prompt = promptAssembler.assemble(state, List.of(turn), ambient);

        long start = System.currentTimeMillis();
        DeepSeekChatResponse response;
        int toolCalls = 0;
        try {
            response = callModel(key, prompt, null);
        } catch (Exception e) {
            log.error("AI模型调用失败: {}", key.cacheKey(), e);
            sessionService.appendUser(state, incoming);
            sessionService.recordUsage(state, AiChatSettings.model(), 0, 0, 0, 0,
                    (int) (System.currentTimeMillis() - start), 0, false, e.getClass().getSimpleName());
            return;
        }

        AiReply reply = AiReplyParser.parse(response.getContent());
        if (reply.getType() == AiReply.Type.INVALID && "empty_content".equals(reply.getError())) {
            // JSON 模式偶发空内容，重试一次
            response = callModel(key, prompt, null);
            reply = AiReplyParser.parse(response.getContent());
        }
        if (reply.getType() == AiReply.Type.INVALID) {
            // 修复提示重试一次
            prompt.add(DeepSeekMessage.user(PromptTemplates.REPAIR));
            response = callModel(key, prompt, null);
            reply = AiReplyParser.parse(response.getContent());
        }

        int steps = 0;
        while (reply.isCall() && steps < AiChatSettings.agentMaxSteps()
                && toolCalls < AiChatSettings.toolCallsPerTurn()) {
            steps++;
            toolCalls++;
            String result = toolService.execute(turn.getBot(), state, reply.getCall());
            if (StringUtils.isBlank(result)) {
                result = "工具没有返回内容。";
            }
            prompt.addToolResult(StringUtils.defaultString(response.getContent()), result);
            try {
                response = callModel(key, prompt, null);
            } catch (Exception e) {
                log.error("AI工具后调用模型失败: {}", key.cacheKey(), e);
                break;
            }
            reply = AiReplyParser.parse(response.getContent());
            if (reply.getType() == AiReply.Type.INVALID) {
                prompt.add(DeepSeekMessage.user(PromptTemplates.REPAIR));
                response = callModel(key, prompt, null);
                reply = AiReplyParser.parse(response.getContent());
            }
        }

        if (reply.isCall()) {
            prompt.add(DeepSeekMessage.user("不要再调用工具了，请直接输出最终 JSON（type=say 或 silent）。"));
            try {
                response = callModel(key, prompt, null);
                reply = AiReplyParser.parse(response.getContent());
            } catch (Exception e) {
                log.warn("AI强制收尾调用失败: {}", key.cacheKey(), e);
                reply = AiReply.silent();
            }
        }

        if (state.generation() != generation) {
            log.debug("AI会话已被重置，丢弃本轮结果: {}", key.cacheKey());
            return;
        }

        sessionService.appendUser(state, incoming);
        List<AiReplyMessage> messages = List.of();
        if (reply.isSay()) {
            messages = AiReplySanitizer.clean(reply.getMessages(), key);
        }
        if (!messages.isEmpty()) {
            sendService.send(turn.getBot(), state, key, messages, incoming, ambient);
        } else if (reply.isInvalid()) {
            String fallback = fallbackText(response.getContent());
            if (StringUtils.isNotBlank(fallback)) {
                AiReplyMessage message = new AiReplyMessage();
                message.setText(fallback);
                sendService.send(turn.getBot(), state, key, List.of(message), incoming, ambient);
            }
        }

        DeepSeekUsage usage = response.getUsage();
        sessionService.recordUsage(state, response.getModel(), usage == null ? 0 : usage.getPromptTokens(),
                usage == null ? 0 : usage.getCompletionTokens(), usage == null ? 0 : usage.getTotalTokens(),
                usage == null ? 0 : usage.getPromptCacheHitTokens(),
                (int) (System.currentTimeMillis() - start), toolCalls, true, null);
        addDailyTokens(usage == null ? 0 : usage.getTotalTokens());
    }

    private DeepSeekChatResponse callModel(AiSessionKey key, AiPrompt prompt, Integer maxTokens) {
        acquire();
        try {
            DeepSeekChatResponse response = deepSeekClient.chat(prompt.getMessages(),
                    DeepSeekClient.hashUserKey(key.cacheKey()), maxTokens);
            if ("length".equalsIgnoreCase(response.getFinishReason())) {
                response = deepSeekClient.chat(prompt.getMessages(),
                        DeepSeekClient.hashUserKey(key.cacheKey()), Math.min(4096, AiChatSettings.maxTokens() * 2));
            }
            return response;
        } finally {
            active.decrementAndGet();
        }
    }

    private void acquire() {
        int limit = AiChatSettings.maxConcurrent();
        long deadline = System.currentTimeMillis() + 30_000;
        while (true) {
            int current = active.get();
            if (current < limit && active.compareAndSet(current, current + 1)) {
                return;
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new IllegalStateException("AI并发等待超时");
            }
            sleep(50);
        }
    }

    private String fallbackText(String content) {
        if (StringUtils.isBlank(content) || content.trim().startsWith("{")) {
            return "";
        }
        List<AiReplyMessage> raw = new ArrayList<>();
        AiReplyMessage message = new AiReplyMessage();
        message.setText(content);
        raw.add(message);
        List<AiReplyMessage> cleaned = AiReplySanitizer.clean(raw, null);
        return cleaned.isEmpty() ? "" : cleaned.get(0).getText();
    }

    private boolean isOverDailyLimit() {
        long limit = AiChatSettings.dailyTokenLimit();
        return limit > 0 && dailyTokens.get() >= limit;
    }

    private void addDailyTokens(int tokens) {
        if (tokens <= 0) {
            return;
        }
        long today = LocalDate.now().toEpochDay();
        if (tokenDay.getAndSet(today) != today) {
            dailyTokens.set(0);
        }
        dailyTokens.addAndGet(tokens);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class SessionQueue {
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final java.util.Deque<AiTurn> pending = new java.util.concurrent.ConcurrentLinkedDeque<>();
    }
}
