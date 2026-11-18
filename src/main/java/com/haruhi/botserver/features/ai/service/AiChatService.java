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
import com.haruhi.botserver.infrastructure.logging.DbLog;
import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
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
public class AiChatService {

    private final Map<AiSessionKey, SessionQueue> queues = new ConcurrentHashMap<>();

    private final AiSessionService sessionService;
    private final AiContextService contextService;
    private final PromptAssembler promptAssembler;
    private final DeepSeekClient deepSeekClient;
    private final AiSendService sendService;
    private final AiToolService toolService;
    private final AiMemoryService memoryService;

    private final AtomicInteger active = new AtomicInteger();
    private final AtomicLong dailyTokens = new AtomicLong();
    private final AtomicLong tokenDay = new AtomicLong(LocalDate.now().toEpochDay());

    public AiChatService(AiSessionService sessionService,
                         AiContextService contextService,
                         PromptAssembler promptAssembler,
                         DeepSeekClient deepSeekClient,
                         AiSendService sendService,
                         AiToolService toolService,
                         AiMemoryService memoryService) {
        this.sessionService = sessionService;
        this.contextService = contextService;
        this.promptAssembler = promptAssembler;
        this.deepSeekClient = deepSeekClient;
        this.sendService = sendService;
        this.toolService = toolService;
        this.memoryService = memoryService;
    }

    public void enqueue(AiSessionKey key, AiTurn turn) {
        if (key == null || turn == null || turn.getIncoming() == null) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI入队参数不完整，已丢弃 key:{} turnNull:{} incomingNull:{}",
                    key, turn == null, turn == null || turn.getIncoming() == null);
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
                        DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI会话队列积压，丢弃最旧一轮 key:{} userId:{}",
                                key.cacheKey(), dropped.userId());
                    }
                }
            }
            submit = !queue.running.get();
            if (submit) {
                queue.running.set(true);
            }
        }
        if (submit) {
            try {
                ThreadPoolUtil.getHandleCommandPool().execute(() -> work(key, queue));
            } catch (Exception e) {
                synchronized (queue) {
                    queue.running.set(false);
                }
                DbLog.error(BusinessModuleEnum.AI_CHAT, "AI会话worker提交失败 key:{}", key.cacheKey(), e);
            }
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
        a.getImageUrls().addAll(b.getImageUrls());
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
                    DbLog.error(BusinessModuleEnum.AI_CHAT, "处理AI会话轮次异常 key:{}", key.cacheKey(), e);
                }
            }
        } catch (Exception e) {
            DbLog.error(BusinessModuleEnum.AI_CHAT, "AI会话worker异常 key:{}", key.cacheKey(), e);
        } finally {
            synchronized (queue) {
                queue.running.set(false);
                if (!queue.pending.isEmpty() && queue.running.compareAndSet(false, true)) {
                    try {
                        ThreadPoolUtil.getHandleCommandPool().execute(() -> work(key, queue));
                    } catch (Exception e) {
                        queue.running.set(false);
                        DbLog.error(BusinessModuleEnum.AI_CHAT, "AI会话worker重新提交失败 key:{}", key.cacheKey(), e);
                    }
                }
            }
        }
    }

    private void process(AiSessionKey key, AiTurn turn) {
        if (isOverDailyLimit()) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI每日token限额已到，跳过本轮 key:{} limit:{} used:{}",
                    key.cacheKey(), AiChatSettings.dailyTokenLimit(), dailyTokens.get());
            return;
        }
        AiSessionState state = sessionService.load(key);
        if (state.paused()) {
            DbLog.debug(BusinessModuleEnum.AI_CHAT, "AI会话已暂停，丢弃本轮 key:{}", key.cacheKey());
            return;
        }
        if (turn.getBot() == null) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI轮次缺少机器人连接，丢弃本轮 key:{}", key.cacheKey());
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
            DbLog.error(BusinessModuleEnum.AI_CHAT, "AI模型调用失败 key:{} model:{} elapsed:{}ms",
                    key.cacheKey(), AiChatSettings.model(), System.currentTimeMillis() - start, e);
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
            if (reply.getType() == AiReply.Type.INVALID) {
                DbLog.warn(BusinessModuleEnum.AI_CHAT,
                        "AI模型输出协议解析失败 key:{} error:{} finishReason:{} content:{}",
                        key.cacheKey(), reply.getError(), response.getFinishReason(), snippet(response.getContent()));
            }
        }

        int steps = 0;
        state.setToolContext(incoming.getReplyMessageId(), turn.isDirect());
        try {
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
                    DbLog.error(BusinessModuleEnum.AI_CHAT, "AI工具后调用模型失败 key:{} tool:{} steps:{}",
                            key.cacheKey(), reply.getCall().getName(), steps, e);
                    break;
                }
                reply = AiReplyParser.parse(response.getContent());
                if (reply.getType() == AiReply.Type.INVALID) {
                    prompt.add(DeepSeekMessage.user(PromptTemplates.REPAIR));
                    response = callModel(key, prompt, null);
                    reply = AiReplyParser.parse(response.getContent());
                }
            }
        } finally {
            state.clearToolContext();
        }

        if (reply.isCall()) {
            DbLog.warn(BusinessModuleEnum.AI_CHAT,
                    "AI工具循环达到上限，强制收尾 key:{} steps:{} toolCalls:{} maxSteps:{} maxToolCalls:{} tool:{}",
                    key.cacheKey(), steps, toolCalls, AiChatSettings.agentMaxSteps(),
                    AiChatSettings.toolCallsPerTurn(), reply.getCall().getName());
            prompt.add(DeepSeekMessage.user("不要再调用工具了，请直接输出最终 JSON（type=say 或 silent）。"));
            try {
                response = callModel(key, prompt, null);
                reply = AiReplyParser.parse(response.getContent());
            } catch (Exception e) {
                DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI强制收尾调用失败 key:{}", key.cacheKey(), e);
                reply = AiReply.silent();
            }
        }

        if (state.generation() != generation) {
            DbLog.debug(BusinessModuleEnum.AI_CHAT, "AI会话已被重置，丢弃本轮结果 key:{}", key.cacheKey());
            return;
        }

        if (AiChatSettings.memoryAutoSave()) {
            memoryService.rememberAll(state, reply.getMemory());
            memoryService.forgetAll(state, reply.getForgetMemory());
        }

        sessionService.appendUser(state, incoming);
        List<AiReplyMessage> messages = List.of();
        if (reply.isSay()) {
            messages = AiReplySanitizer.clean(reply.getMessages(), key);
            if (messages.isEmpty()) {
                DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI回复内容清洗后为空，未发送 key:{} rawCount:{}",
                        key.cacheKey(), reply.getMessages() == null ? 0 : reply.getMessages().size());
            }
        }
        if (!messages.isEmpty()) {
            sendService.send(turn.getBot(), state, key, messages, incoming, ambient);
        } else if (reply.isInvalid()) {
            String fallback = fallbackText(response.getContent());
            if (StringUtils.isNotBlank(fallback)) {
                AiReplyMessage message = new AiReplyMessage();
                message.setText(fallback);
                sendService.send(turn.getBot(), state, key, List.of(message), incoming, ambient);
            } else {
                DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI回复无效且无可用兜底文本，未发送 key:{} error:{} content:{}",
                        key.cacheKey(), reply.getError(), snippet(response.getContent()));
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
                DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI输出被截断(finish_reason=length)，扩大maxTokens重试 key:{}",
                        key.cacheKey());
                response = deepSeekClient.chat(prompt.getMessages(),
                        DeepSeekClient.hashUserKey(key.cacheKey()), Math.min(4096, AiChatSettings.maxTokens() * 2));
                if ("length".equalsIgnoreCase(response.getFinishReason())) {
                    DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI输出二次重试后仍被截断 key:{}", key.cacheKey());
                }
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
                DbLog.warn(BusinessModuleEnum.AI_CHAT, "AI并发等待超时 limit:{} active:{}", limit, current);
                throw new IllegalStateException("AI并发等待超时");
            }
            sleep(50);
        }
    }

    private String snippet(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        String text = content.replaceAll("\\s+", " ").trim();
        return text.length() <= 200 ? text : text.substring(0, 200) + "…";
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
