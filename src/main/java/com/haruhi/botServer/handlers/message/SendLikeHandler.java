package com.haruhi.botServer.handlers.message;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.entity.SendLikeRecord;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.sendLikeRecord.SendLikeRecordService;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.WsSyncRequestUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    private static final int TIMES = 10;

    @Override
    public boolean onMessage(WebSocketSession session, Message message) {
        MatchResult result = matches(message);
        if(!result.isMatched()){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                if(isLiked(message.getUserId(), message.getSelfId())){
                    Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),"今日已给你点赞过啦",true);
                    return;
                }

                SyncResponse sendLikeRes = WsSyncRequestUtil.sendLike(session, message.getUserId(), TIMES, 10 * 1000);
                log.info("发送点赞响应：{}", JSONObject.toJSONString(sendLikeRes));
                if(sendLikeRes.isSuccess()){
                    Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),"攒了你"+TIMES+"次哦，记得回赞",true);
                    record(message);
                    return;
                }

                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),
                        StringUtils.isNotBlank(sendLikeRes.getMessage())
                                ? sendLikeRes.getMessage() : StringUtils.isNotBlank(sendLikeRes.getWording())
                                ? sendLikeRes.getWording() : "点赞失败"
                        ,true);
            }catch (Exception e){
                log.error("点赞异常",e);
                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),
                        "点赞异常\n"+e.getMessage(),
                        true);
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

    private void record(Message message){
        SendLikeRecord record = new SendLikeRecord();
        record.setUserId(message.getUserId());
        record.setSelfId(message.getSelfId());
        record.setSendTime(new Date());
        record.setTimes(TIMES);
        record.setMessageType(message.getMessageType());
        sendLikeRecordService.save(record);
    }

    private boolean isLiked(Long userId, Long selfId){
        Date d = new Date();
        int count = sendLikeRecordService.count(new LambdaQueryWrapper<SendLikeRecord>()
                .eq(SendLikeRecord::getUserId, userId)
                .eq(SendLikeRecord::getSelfId, selfId)
                .between(SendLikeRecord::getSendTime, DateTimeUtil.getStartOfDay(d), DateTimeUtil.getEndOfDay(d)));
        return count > 0;
    }
}
