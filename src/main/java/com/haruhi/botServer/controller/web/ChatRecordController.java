package com.haruhi.botServer.controller.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageInfo;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.entity.ChatRecordExtendV2;
import com.haruhi.botServer.mapper.ChatRecordExtendV2Mapper;
import com.haruhi.botServer.service.ChatRecordService;
import com.haruhi.botServer.vo.HttpResp;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/chatRecord")
public class ChatRecordController {
    @Autowired
    private ChatRecordExtendV2Mapper chatRecordExtendV2Mapper;
    @Autowired
    private ChatRecordService chatRecordService;


    @PostMapping("/v2/search")
    public HttpResp<PageInfo> listV2(@RequestBody ChatRecordQueryReq request){
        if (StringUtils.isBlank(request.getMessageType())
                || (MessageTypeEnum.group.getType().equals(request.getMessageType()) && Objects.isNull(request.getGroupId()))) {
            return HttpResp.fail("参数错误",null);
        }
        if (MessageTypeEnum.privat.getType().equals(request.getMessageType()) && Objects.isNull(request.getSelfId())) {
            return HttpResp.fail("参数错误",null);
        }
        return HttpResp.success(chatRecordService.search(request, true, true));
    }


    @PostMapping("/v2/extend")
    public HttpResp<ChatRecordExtendV2> selectExtendV2(@RequestBody ChatRecordQueryReq request){
        if (Objects.isNull(request.getChatId()) || Objects.isNull(request.getUserId())) {
            return HttpResp.fail("参数错误",null);
        }
        ChatRecordExtendV2 extendV2 = chatRecordExtendV2Mapper.selectOne(new LambdaQueryWrapper<ChatRecordExtendV2>()
                .eq(ChatRecordExtendV2::getChatRecordId, request.getChatId())
                .eq(ChatRecordExtendV2::getUserId, request.getUserId())
                .last("limit 1"));
        return HttpResp.success(extendV2);
    }
}
