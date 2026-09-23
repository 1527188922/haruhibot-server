package com.haruhi.botserver.administration.controller;

import com.haruhi.botserver.shared.annotation.IgnoreAuthentication;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.integration.onebot.model.MessageTypeEnum;
import com.haruhi.botserver.features.chatrecord.service.ChatRecordService;
import com.haruhi.botserver.shared.model.HttpResp;
import com.haruhi.botserver.infrastructure.image.WebpDiagnostics;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(SysConstants.CONTEXT_PATH+"/test")
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

    /** Optional upload: return environment only when file is absent or empty. */
    @IgnoreAuthentication
    @PostMapping("/webp")
    public HttpResp<Map<String, Object>> webp(@RequestParam(value = "file", required = false) MultipartFile file) {
        return HttpResp.success("WebP 诊断完成，请查看各步骤的 success 和 error", WebpDiagnostics.inspect(file));
    }

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
