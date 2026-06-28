package com.haruhi.botServer.handler.message;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.github.pagehelper.PageInfo;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.entity.vo.ChatRecordVo;
import com.haruhi.botServer.service.ChatRecordService;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import com.haruhi.botServer.ws.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Component
public class WhoTaggedMeHandler  implements IGroupMessageHandler {
    // 查询近24小时是否有人at
    public static final int OFFSET_HOURS = -48;


    @Autowired
    private ChatRecordService chatRecordService;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_530.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_530.getName();
    }


    @Override
    public boolean onGroup(Bot bot, Message message) {
        String text = message.getText(0);
        if (!(StringUtils.isNotBlank(text) && "谁at我".equalsIgnoreCase(text.trim()))) {
            return false;
        }
        ChatRecordQueryReq chatRecordQueryReq = new ChatRecordQueryReq();
        chatRecordQueryReq.setMessageType(message.getMessageType());
        chatRecordQueryReq.setGroupId(message.getGroupId());
        chatRecordQueryReq.setStartTime(
                DateUtil.format(
                        DateUtil.offsetHour(new DateTime(), OFFSET_HOURS),
                        DateTimeUtil.PatternEnum.yyyyMMddHHmmss.getPattern()
                )
        );
        chatRecordQueryReq.setPageSize(1);
        chatRecordQueryReq.setContent(KQCodeUtils.INSTANCE.toCq(
                CqCodeTypeEnum.at.getType(),
                new HashMap<>(1){{
                    put("qq",message.getUserId());
                }}
        ));
        PageInfo<ChatRecordVo> pageInfo = chatRecordService.search(chatRecordQueryReq, true, false);

        List<ChatRecordVo> list = pageInfo.getList();
        if (list.isEmpty()) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText("最近好像没人at你。。"));
            return true;
        }
        ChatRecordVo first = list.getFirst();

        List<MessageHolder> messageHolders = MessageHolder.instanceReply(first.getMessageId());
        messageHolders.addAll(MessageHolder.instanceText("这里有人at你"));
        bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), messageHolders);
        return true;
    }
}
