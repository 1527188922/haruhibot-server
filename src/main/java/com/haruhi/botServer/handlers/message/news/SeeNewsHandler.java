package com.haruhi.botServer.handlers.message.news;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.news.response.NewsBy163Resp;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.news.NewsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

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
    private NewsService NewsService;

    @Override
    public boolean onMessage(final WebSocketSession session,final Message message) {
        if(!message.getRawMessage().matches(RegexEnum.SEE_TODAY_NEWS.getValue())){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                List<NewsBy163Resp> newsBy163Resps = NewsService.requestNewsBy163();
                if(CollectionUtils.isEmpty(newsBy163Resps)){
                    return;
                }
                if (message.isGroupMsg()) {
                    NewsService.sendGroup(session,newsBy163Resps,message.getGroupId());
                }else if (message.isPrivateMsg()){
                    NewsService.sendPrivate(session,newsBy163Resps,message.getUserId());
                }
            }catch (Exception e){
                log.error("查看今日新闻异常",e);
            }
        });
        return true;
    }
}
