package com.haruhi.botServer.handlers.message;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.entity.SendLikeRecordSqlite;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.SendLikeRecordSqliteService;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private SendLikeRecordSqliteService sendLikeRecordService;

    private static final int TIMES = 10;

    @Override
    public boolean onMessage(Bot bot, Message message) {
        MatchResult result = matches(message);
        if(!result.isMatched()){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                if(isLiked(message.getUserId(), message.getSelfId())){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"今日已给你点赞过啦",true);
                    return;
                }

                SyncResponse<String> sendLikeRes = bot.sendLike(message.getUserId(), TIMES, 10 * 1000);
                log.info("发送点赞响应：{}", JSONObject.toJSONString(sendLikeRes));
                if(sendLikeRes.isSuccess()){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"攒了你"+TIMES+"次哦，记得回赞",true);
                    record(message);
                    return;
                }

                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        StringUtils.isNotBlank(sendLikeRes.getMessage())
                                ? sendLikeRes.getMessage() : StringUtils.isNotBlank(sendLikeRes.getWording())
                                ? sendLikeRes.getWording() : "点赞失败"
                        ,true);
            }catch (Exception e){
                log.error("点赞异常",e);
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        "点赞异常\n"+e.getMessage(),
                        true);
            }
        });
        return true;
    }
    private MatchResult matches(Message message){
        if(!(message.isTextMsgOnly() || (message.isTextMsg() && message.isAtBot()))){
            return MatchResult.unmatched();
        }
        if("赞我".equals(message.getText(0).trim())){
            return MatchResult.matched();
        }
        return MatchResult.unmatched();
    }

    private void record(Message message){
        SendLikeRecordSqlite record = new SendLikeRecordSqlite();
        record.setUserId(message.getUserId());
        record.setSelfId(message.getSelfId());
        record.setSendTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
        record.setTimes(TIMES);
        record.setMessageType(message.getMessageType());
        sendLikeRecordService.save(record);
    }

    private boolean isLiked(Long userId, Long selfId){
        Date d = new Date();
        int count = sendLikeRecordService.count(new LambdaQueryWrapper<SendLikeRecordSqlite>()
                .eq(SendLikeRecordSqlite::getUserId, userId)
                .eq(SendLikeRecordSqlite::getSelfId, selfId)
                .between(SendLikeRecordSqlite::getSendTime,
                        DateTimeUtil.dateTimeFormat(DateTimeUtil.getStartOfDay(d), DateTimeUtil.PatternEnum.yyyyMMddHHmmss),
                        DateTimeUtil.dateTimeFormat(DateTimeUtil.getEndOfDay(d), DateTimeUtil.PatternEnum.yyyyMMddHHmmss)));
        return count > 0;
    }
}
