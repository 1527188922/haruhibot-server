package com.haruhi.botServer.controller;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketSession;

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
        if(Server.getConnections() == 0){
            return HttpResp.fail("暂无连接",null);
        }

        WebSocketSession session = null;
        if (Objects.isNull(botId)) {
            session = Server.getSession();
        }else{
            session = Server.getSessionByBot(botId);
        }
        if(session == null){
            return HttpResp.fail("session不存在",null);
        }
        if("1".equals(sync)){
            SyncResponse response = Server.sendSyncMessage(session, userId, groupId, enumByType.getType(), botId, "haruhi", Arrays.asList(msg), 30 * 1000);
            log.info("同步发送响应 {}",JSONObject.toJSONString(response));
            return HttpResp.success("已发送",response);
        }
        Server.sendMessage(session, userId, groupId, enumByType.getType(), msg, false);
        return HttpResp.success("已发送",null);
    }
    
}
