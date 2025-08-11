package com.haruhi.botServer.handlers.message.news;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.news.response.NewsBy163Resp;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.news.NewsService;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SeeNewsHandler implements IAllMessageEvent {

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
                List<NewsBy163Resp> newsBy163Resps = newsService.requestNewsBy163();
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
