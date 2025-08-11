package com.haruhi.botServer.controller.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.entity.ChatRecordExtendSqlite;
import com.haruhi.botServer.entity.ChatRecordSqlite;
import com.haruhi.botServer.mapper.ChatRecordExtendSqliteMapper;
import com.haruhi.botServer.service.ChatRecordSqliteService;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/chatRecord")
public class ChatRecordController {

    @Autowired
    private ChatRecordSqliteService chatRecordSqliteService;
    @Autowired
    private ChatRecordExtendSqliteMapper chatRecordExtendSqliteMapper;


    @PostMapping("/search")
    public HttpResp<IPage<ChatRecordSqlite>> list(@RequestBody ChatRecordQueryReq request){
        IPage<ChatRecordSqlite> page = chatRecordSqliteService.search(request, true);
        return HttpResp.success(page);
    }

    @PostMapping("/extend")
    public HttpResp<ChatRecordExtendSqlite> selectExtend(@RequestBody ChatRecordSqlite request){
        ChatRecordExtendSqlite recordExtendSqlite = chatRecordExtendSqliteMapper.selectOne(new LambdaQueryWrapper<ChatRecordExtendSqlite>()
                .eq(ChatRecordExtendSqlite::getChatRecordId, request.getId())
                .last("limit 1"));
        return HttpResp.success(recordExtendSqlite);
    }
}
