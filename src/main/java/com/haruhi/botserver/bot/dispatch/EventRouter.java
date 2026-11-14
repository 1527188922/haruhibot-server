package com.haruhi.botserver.bot.dispatch;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.infrastructure.logging.DbLog;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.integration.onebot.model.PostTypeEnum;
import com.haruhi.botserver.integration.onebot.model.event.MetaEvent;
import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;
import com.haruhi.botserver.integration.onebot.model.event.RequestEvent;
import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 事件路由
 * 按 post_type 把 ws 上报的 payload 解析成对应的事件模型并分发给对应的分发器
 * 文档：https://napneko.github.io/onebot/basic_event
 */
@Slf4j
@Component
public class EventRouter {

    private final MessageDispatcher messageDispatcher;
    private final NoticeDispatcher noticeDispatcher;
    private final RequestDispatcher requestDispatcher;
    private final MetaEventDispatcher metaEventDispatcher;

    public EventRouter(MessageDispatcher messageDispatcher,
                       NoticeDispatcher noticeDispatcher,
                       RequestDispatcher requestDispatcher,
                       MetaEventDispatcher metaEventDispatcher) {
        this.messageDispatcher = messageDispatcher;
        this.noticeDispatcher = noticeDispatcher;
        this.requestDispatcher = requestDispatcher;
        this.metaEventDispatcher = metaEventDispatcher;
    }

    public void route(Bot bot, JSONObject json, String raw) {
        String postType = json.getString("post_type");
        if (StringUtils.isBlank(postType)) {
            log.warn("收到未知上报数据，缺少post_type：{}", raw);
            return;
        }
        try {
            if (PostTypeEnum.message.name().equals(postType)
                    || PostTypeEnum.message_sent.name().equals(postType)) {
                handleMessage(bot, json, postType, raw);
            } else if (PostTypeEnum.notice.name().equals(postType)) {
                NoticeEvent event = json.toJavaObject(NoticeEvent.class);
                submit(() -> noticeDispatcher.onEvent(bot, event), postType, raw);
            } else if (PostTypeEnum.request.name().equals(postType)) {
                RequestEvent event = json.toJavaObject(RequestEvent.class);
                submit(() -> requestDispatcher.onEvent(bot, event), postType, raw);
            } else if (PostTypeEnum.meta_event.name().equals(postType)) {
                MetaEvent event = json.toJavaObject(MetaEvent.class);
                submit(() -> metaEventDispatcher.onEvent(bot, event), postType, raw);
            } else {
                log.info("未知PostType: {}，payload：{}", postType, raw);
            }
        } catch (Exception e) {
            DbLog.error(BusinessModuleEnum.BOT_WS, "解析/分发事件异常 postType:" + postType + " payload:" + raw, e);
        }
    }

    private void handleMessage(Bot bot, JSONObject json, String postType, String raw) {
        // 部分实现（如 bot_offline）的 message 字段是字符串，
        // 而 Message.message 是 List<MessageHolder>，这里统一转成文本消息段
        Object message = json.get("message");
        if (message instanceof String s) {
            json.put("message", MessageHolder.instanceText(s));
        }
        Message bean = json.toJavaObject(Message.class);
        bean.setRawWsMsg(raw);
        if (bean.getRawMessage() == null) {
            return;
        }
        submit(() -> messageDispatcher.onEvent(bot, bean), postType, raw);
    }

    /**
     * 非消息类事件放到线程池处理，避免阻塞 ws 读取线程
     */
    private void submit(Runnable task, String postType, String raw) {
        ThreadPoolUtil.getHandleCommandPool().execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                DbLog.error(BusinessModuleEnum.BOT_WS,
                        "处理事件异常 postType:" + postType + " payload:" + raw, e);
            }
        });
    }
}
