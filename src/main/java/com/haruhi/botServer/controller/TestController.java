package com.haruhi.botServer.controller;

import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.service.ChatRecordService;
import com.haruhi.botServer.vo.HttpResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/test")
public class TestController {

    @Autowired
    private ChatRecordService chatRecordService;

    @IgnoreAuthentication
    @RequestMapping("/ping")
    public HttpResp<Map<String,Object>> ping(){
        HashMap<String, Object> hashMap = new HashMap<String, Object>() {{
            put("time",ZonedDateTime.now());
        }};
        return HttpResp.success("pong",hashMap);
    }

    @IgnoreAuthentication
    @PostMapping("/migrateData")
    public HttpResp<String> migrateData(@RequestParam("type") String messageType,
                                        @RequestParam(value = "groupId",required = false) Long groupId,
                                        @RequestParam(value = "selfId",required = false) Long selfId){
        Long resid = null;
        if (MessageTypeEnum.group.getType().equals(messageType)) {
            chatRecordService.migrateGroupData(groupId);
            resid = groupId;
        }else if (MessageTypeEnum.privat.getType().equals(messageType)) {
            chatRecordService.migratePrivateData(selfId);
            resid = selfId;
        }
        return HttpResp.success(messageType+":"+resid,"migrated");
    }
}
