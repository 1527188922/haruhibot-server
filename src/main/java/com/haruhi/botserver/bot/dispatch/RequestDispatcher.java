package com.haruhi.botserver.bot.dispatch;

import com.haruhi.botserver.bot.handler.IRequestHandler;
import com.haruhi.botserver.integration.onebot.model.event.RequestEvent;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 请求事件分发器（加好友/加群）
 * 目前没有具体实现类，后续需要自动处理请求时实现 IRequestHandler 即可
 */
@Slf4j
@Component
public class RequestDispatcher {

    private final List<IRequestHandler> container = new ArrayList<>();

    public RequestDispatcher(Map<String, IRequestHandler> handlerMap) {
        if (!CollectionUtils.isEmpty(handlerMap)) {
            container.addAll(handlerMap.values());
            log.info("加载了{}个请求处理类", container.size());
        }
    }

    public void onEvent(Bot bot, RequestEvent event) {
        log.info("收到请求事件：requestType：{}，subType：{}，userId：{}，groupId：{}，flag：{}，comment：{}",
                event.getRequestType(), event.getSubType(), event.getUserId(), event.getGroupId(),
                event.getFlag(), event.getComment());
        for (IRequestHandler handler : container) {
            try {
                if (handler.support(event)) {
                    handler.onRequest(bot, event);
                }
            } catch (Exception e) {
                log.error("处理请求事件异常 requestType:{} subType:{} handler:{}",
                        event.getRequestType(), event.getSubType(), handler.getClass().getSimpleName(), e);
            }
        }
    }
}
