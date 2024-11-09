package com.haruhi.botServer.handlers.message;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.sendLikeRecord.SendLikeRecordService;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.WsSyncRequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;

@Slf4j
@Component
public class SendLikeHandler implements IAllMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_550.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_550.getName();
    }

    @Autowired
    private SendLikeRecordService sendLikeRecordService;

    @Override
    public boolean onMessage(WebSocketSession session, Message message) {
        MatchResult result = matches(message);
        if(!result.isMatched()){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {



                SyncResponse sendLikeRes = WsSyncRequestUtil.sendLike(session, message.getUserId(), 10, 10 * 1000);
                log.info("发送点赞响应：{}", JSONObject.toJSONString(sendLikeRes));
            }catch (Exception e){
                log.error("error",e);
            }
        });
        return true;
    }
    private MatchResult matches(Message message){
        if(!message.isTextMsg()){
            return MatchResult.unmatched();
        }
        if("赞我".equals(message.getText(0).trim())){
            return MatchResult.matched();
        }
        return MatchResult.unmatched();
    }

    private boolean isLiked(Long userId, Date date){
        return false;
    }
}
