package com.haruhi.botServer.controller.web;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.entity.ChatRecordExtendV2;
import com.haruhi.botServer.entity.vo.ChatRecordVo;
import com.haruhi.botServer.service.ChatRecordService;
import com.haruhi.botServer.vo.CodeNameReq;
import com.haruhi.botServer.vo.GroupChatUserResp;
import com.haruhi.botServer.vo.HttpResp;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/chatRecord")
public class ChatRecordController{
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

    @PostMapping("/group/user")
    public HttpResp<PageInfo<GroupChatUserResp>> queryUserInGroup(@RequestBody CodeNameReq req){
        return HttpResp.success(chatRecordService.queryUser(req));
    }

    @PostMapping("/v2/extend")
    public HttpResp<ChatRecordExtendV2> selectExtendV2(@RequestBody ChatRecordQueryReq request){
        if (Objects.isNull(request.getChatId()) || Objects.isNull(request.getUserId())) {
            return HttpResp.fail("参数错误",null);
        }
        ChatRecordExtendV2 chatRecordExtendV2 = chatRecordService.selectChatRecordExtendOne(request.getChatId(), request.getUserId());
        return HttpResp.success(chatRecordExtendV2);
    }

    @PostMapping("/context")
    public HttpResp<List<ChatRecordVo>> selectExtendV2(@RequestBody JSONObject request){
        Long id = request.getLong("id");
        if (Objects.isNull(id)) {
            return HttpResp.fail("缺失消息id",null);
        }

        String messageType = request.getString("messageType");
        MessageTypeEnum enumByType = MessageTypeEnum.getEnumByType(messageType);
        if (enumByType == null) {
            return HttpResp.fail("消息类型错误："+messageType,null);
        }

        long offset1 = request.getLongValue("offset1");
        long offset2 = request.getLongValue("offset2");


        if (MessageTypeEnum.group == enumByType) {
            Long groupId = request.getLong("groupId");
            if (Objects.isNull(groupId)) {
                return HttpResp.fail("缺失群号",null);
            }
            List<ChatRecordVo> chatRecordVos = chatRecordService.groupMsgContext(groupId, id, offset1, offset2);
            return HttpResp.success(chatRecordVos);
        }
        Long selfId = request.getLong("selfId");
        Long targetId = request.getLong("targetId");
        if (Objects.isNull(selfId)) {
            return HttpResp.fail("参数错误",null);
        }
        if (Objects.isNull(targetId)) {
            return HttpResp.fail("未获取到targetId，该消息不支持定位",null);
        }
        List<ChatRecordVo> chatRecordVos = chatRecordService.privateMsgContext(selfId, targetId, id, offset1, offset2);
        return HttpResp.success(chatRecordVos);
    }

}
