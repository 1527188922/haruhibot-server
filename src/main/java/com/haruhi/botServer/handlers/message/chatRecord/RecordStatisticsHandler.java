package com.haruhi.botServer.handlers.message.chatRecord;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.request.ForwardMsgItem;
import com.haruhi.botServer.dto.gocq.response.GroupMember;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.ChatRecord;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.mapper.ChatRecordMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.WsSyncRequestUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Component
@Slf4j
public class RecordStatisticsHandler implements IGroupMessageEvent {
    
    @Autowired
    private ChatRecordMapper chatRecordMapper;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_250.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_250.getName();
    }
    
    @Override
    public boolean onGroup(WebSocketSession session, Message message) {
        
        if(!message.getRawMessage().matches(RegexEnum.RECORD_STATISTICS.getValue())){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                long l = System.currentTimeMillis();
                List<ChatRecord> chatRecords = chatRecordMapper.groupRecordCounting(message.getGroupId(), message.getSelfId());
                log.info("聊天记录分组执行sql cost:{}",System.currentTimeMillis() - l);
                if(CollectionUtils.isEmpty(chatRecords)){
                    Server.sendGroupMessage(session,message.getGroupId(),"暂无聊天记录",true);
                    return;
                }

                List<GroupMember> groupMemberList = WsSyncRequestUtil.getGroupMemberList(session, message.getGroupId(), Collections.singletonList(message.getSelfId()), 10 * 1000);

                List<ForwardMsgItem> params = new ArrayList<>(chatRecords.size() + 1);

                ChatRecord chatRecord = chatRecordMapper.selectOne(new LambdaQueryWrapper<ChatRecord>()
                        .select(ChatRecord::getTime)
                        .eq(ChatRecord::getGroupId, message.getGroupId())
                        .eq(ChatRecord::getSelfId, message.getSelfId())
                        .eq(ChatRecord::getMessageType, MessageTypeEnum.group.getType())
                        .eq(ChatRecord::getDeleted, false)
                        .orderByAsc(ChatRecord::getTime)
                        .last("LIMIT 1"));
                if(chatRecord != null && chatRecord.getTime() != null){
                    
                    params.add(new ForwardMsgItem(new ForwardMsgItem.Data(BotConfig.NAME, message.getSelfId(), 
                            "从[" + DateTimeUtil.dateTimeFormat(chatRecord.getTime(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss) + "]开始统计")));
                }

                for (int i = 0; i < chatRecords.size(); i++) {
                    ChatRecord item = chatRecords.get(i);
                    String name = getName(item, groupMemberList);
                    String imageCq = KQCodeUtils.getInstance().toCq(CqCodeTypeEnum.image.getType(), 
                            "file=" + CommonUtil.getAvatarUrl(item.getUserId(), false));
                    String msg = imageCq
                            + (i + 1) + "\n" 
                            + name + "(" +item.getUserId() + ")"
                            + "\n发言数：" + item.getTotal();
                    params.add(new ForwardMsgItem(new ForwardMsgItem.Data(name,item.getUserId(), msg)));
                }
                List<List<ForwardMsgItem>> lists = CommonUtil.averageAssignList(params, 70);
                for (int i = 0; i < lists.size(); i++) {
                    Server.sendGroupMessage(session, message.getGroupId(), lists.get(i));
                    if(i < lists.size() - 1){
                        try {
                            Thread.sleep(2000);
                        }catch (InterruptedException e){}
                    }
                }
            }catch (Exception e){
                log.error("聊天统计异常",e);
                Server.sendGroupMessage(session, message.getGroupId(), "聊天统计异常\n"+e.getMessage(),true);
            }
            
        });
        return true;
    }

    private String getName(ChatRecord e, List<GroupMember> groupMemberList){
        String card = null;
        String nickName = null;
        if(!CollectionUtils.isEmpty(groupMemberList)){
            for (GroupMember groupMember : groupMemberList) {
                if (groupMember.getUserId() != null && groupMember.getUserId().equals(e.getUserId())) {
                    card = groupMember.getCard();
                    nickName = groupMember.getNickname();
                    break;
                }
            }    
        }
        
        if(Strings.isNotBlank(card)){
            return card;
        }
        if(Strings.isNotBlank(nickName)){
            return nickName;
        }

        ChatRecord chatRecord = chatRecordMapper.selectOne(new LambdaQueryWrapper<ChatRecord>()
                .select(ChatRecord::getCard,ChatRecord::getNickname)
                .eq(ChatRecord::getUserId,e.getUserId())
                .eq(ChatRecord::getGroupId, e.getGroupId())
                .eq(ChatRecord::getMessageType, MessageTypeEnum.group.getType())
                .eq(ChatRecord::getDeleted, false)
                .orderByDesc(ChatRecord::getTime)
                .last("LIMIT 1"));
        if(chatRecord != null){
            return StringUtils.isNotBlank(chatRecord.getCard()) ? chatRecord.getCard()
                    : StringUtils.isNotBlank(chatRecord.getNickname()) ? chatRecord.getNickname() : "noname";
        }
        return "noname";
    }
}
