package com.haruhi.botServer.controller;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.BotContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/bot")
public class BotController {
    
    
    @PostMapping("/sendMsg")
    public HttpResp sendMessage(@RequestBody JSONObject jsonObject){
        Long userId = jsonObject.getLong("userId");
        Long groupId = jsonObject.getLong("groupId");
        Long botId = jsonObject.getLong("botId");
        String msg = jsonObject.getString("msg");
        String msgType = jsonObject.getString("msgType");
        String sync = jsonObject.getString("sync");

        MessageTypeEnum enumByType = MessageTypeEnum.getEnumByType(msgType);
        if(enumByType == null){
            return HttpResp.fail("消息类型错误",null);
        }
        if((enumByType == MessageTypeEnum.group && (groupId == null || groupId == 0))
        || (enumByType == MessageTypeEnum.privat && (userId == null || userId == 0))
        || StringUtils.isBlank(msg)){
            return HttpResp.fail("参数错误",null);
        }
        if(BotContainer.getConnections() == 0){
            return HttpResp.fail("暂无连接",null);
        }

        Bot bot = null;
        if (Objects.isNull(botId)) {
            bot = BotContainer.getBotFirst();
        }else{
            bot = BotContainer.getBotById(botId);
        }
        if(bot == null){
            return HttpResp.fail("session不存在",null);
        }
        if("1".equals(sync)){
            SyncResponse response = bot.sendSyncMessage(userId, groupId, enumByType.getType(), botId, "haruhi", Arrays.asList(msg), 30 * 1000);
            log.info("同步发送响应 {}",JSONObject.toJSONString(response));
            return HttpResp.success("已发送",response);
        }
        bot.sendMessage(userId, groupId, enumByType.getType(), msg, false);
        return HttpResp.success("已发送",null);
    }
    
}
