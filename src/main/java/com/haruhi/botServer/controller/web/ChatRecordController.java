package com.haruhi.botServer.controller.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.entity.ChatRecord;
import com.haruhi.botServer.service.chatRecord.ChatRecordService;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/chatRecord")
public class ChatRecordController {

    @Autowired
    private ChatRecordService chatRecordService;


    @PostMapping("/search")
    public HttpResp<IPage<ChatRecord>> list(ChatRecordQueryReq request){
        IPage<ChatRecord> list = chatRecordService.search(request, true);
        return HttpResp.success(list);
    }
}
