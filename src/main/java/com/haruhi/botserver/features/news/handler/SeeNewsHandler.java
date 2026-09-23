package com.haruhi.botserver.features.news.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.ForwardMsgItem;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.features.news.client.model.news163.NewsResp;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.features.news.service.NewsService;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SeeNewsHandler implements IAllMessageHandler {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_400.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_400.getName();
    }
    @Autowired
    private NewsService newsService;

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if(!message.getRawMessage().matches(RegexEnum.SEE_TODAY_NEWS.getValue())){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                List<NewsResp> newsBy163Resps = newsService.requestNewsBy163();
                if(CollectionUtils.isEmpty(newsBy163Resps)){
                    return;
                }

                List<List<MessageHolder>> newsMessages = newsService.createNewsMessage(newsBy163Resps);

                List<ForwardMsgItem> forwardMsgItems = newsMessages.stream()
                        .map(messageHolders -> ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), messageHolders))
                        .collect(Collectors.toList());

                bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgItems);
            }catch (Exception e){
                log.error("查看今日新闻异常",e);
            }
        });
        return true;
    }
}
