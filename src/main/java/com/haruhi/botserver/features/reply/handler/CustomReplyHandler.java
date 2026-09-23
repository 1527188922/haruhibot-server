package com.haruhi.botserver.features.reply.handler;

import com.haruhi.botserver.bot.handler.IAllMessageHandler;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.integration.onebot.model.CqCodeTypeEnum;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.features.reply.persistence.entity.CustomReplySqlite;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.shared.util.CommonUtil;
import com.haruhi.botserver.bot.session.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义回复
 */
@Slf4j
@Component
public class CustomReplyHandler implements IAllMessageHandler {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_180.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_180.getName();
    }

    private final static Map<String, List<CustomReplySqlite>> cache = new HashMap<>();
    public static void putAllCache(Map<String, List<CustomReplySqlite>> other){
        cache.putAll(other);
    }

    public static void clearCache(){
        cache.clear();
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if(! (cache.size() != 0 &&
                ((message.isPrivateMsg() && message.isTextMsgOnly())
                        || (message.isGroupMsg() && message.isTextMsg() && message.isAtBot() && !message.isPicMsg())))){
            return false;
        }

        List<CustomReplySqlite> replyList = null;
        for (Map.Entry<String, List<CustomReplySqlite>> item : cache.entrySet()) {
            if (message.getText(0).trim().matches(item.getKey())) {
                replyList = item.getValue();
                break;
            }
        }
        if(replyList == null){
            return false;
        }

        CustomReplySqlite customReply = replyList.size() == 1 ? replyList.getFirst()
                : replyList.get(CommonUtil.randomInt(0, replyList.size() - 1));

        boolean pass = customReply.pass(message.getMessageType(), 
                message.getGroupId() == null ? null : String.valueOf(message.getGroupId()));
        if(!pass){
            return false;
        }
        
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            if (customReply.getIsText() == 1) {
                if(StringUtils.isBlank(customReply.getReply())){
                    log.error("text类型自定义回复中，reply为空 {}", JSONObject.toJSONString(customReply));
                    return;
                }
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),customReply.getReply(),false);
            }else{
                CqCodeTypeEnum cqCodeTypeEnum = CqCodeTypeEnum.getByType(customReply.getCqType());
                if(cqCodeTypeEnum == null){
                    log.error("非text类型自定义回复中，cqType错误 {}", JSONObject.toJSONString(customReply));
                    return;
                }

                if (StringUtils.isBlank(customReply.getRelativePath()) && StringUtils.isBlank(customReply.getUrl())) {
                    log.error("非text类型自定义回复中，relativePath和url都为空 {}", JSONObject.toJSONString(customReply));
                    return;
                }
                
                String file = StringUtils.isNotBlank(customReply.getAbsolutePath()) ? "file:///" + customReply.getAbsolutePath()
                        : customReply.getUrl();
                String cq = KQCodeUtils.getInstance().toCq(cqCodeTypeEnum.getType(), "file=" + file);

                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),cq,false);
            }
            
        });
        return true;
    }
    
}
