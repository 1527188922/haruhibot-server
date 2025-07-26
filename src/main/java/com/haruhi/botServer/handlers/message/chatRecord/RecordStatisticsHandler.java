package com.haruhi.botServer.handlers.message.chatRecord;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.qqclient.*;
import com.haruhi.botServer.entity.ChatRecordSqlite;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.mapper.ChatRecordSqliteMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Component
@Slf4j
public class RecordStatisticsHandler implements IGroupMessageEvent {
    
    @Autowired
    private ChatRecordSqliteMapper chatRecordSqliteMapper;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_250.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_250.getName();
    }
    
    @Override
    public boolean onGroup(Bot bot, Message message) {
        
        if(!message.getRawMessage().matches(RegexEnum.RECORD_STATISTICS.getValue())){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                long l = System.currentTimeMillis();
                List<ChatRecordSqlite> chatRecords = chatRecordSqliteMapper.groupRecordCounting(message.getGroupId(), message.getSelfId());
                log.info("聊天记录分组执行sql cost:{}",System.currentTimeMillis() - l);
                if(CollectionUtils.isEmpty(chatRecords)){
                    bot.sendGroupMessage(message.getGroupId(),"暂无聊天记录",true);
                    return;
                }

                SyncResponse<List<GroupMember>> syncResponse = bot.getGroupMemberList(message.getGroupId(), 10 * 1000);

                List<GroupMember> groupMemberList = syncResponse.getData();
//                if(!CollectionUtils.isEmpty(groupMemberList)){
//                    List<Long> longs = Collections.singletonList(message.getSelfId());
//                    groupMemberList.removeIf(next -> longs.contains(next.getUserId()));
//                }

                List<ForwardMsgItem> forwardMsgItems = new ArrayList<>(chatRecords.size() + 1);

                ChatRecordSqlite chatRecord = chatRecordSqliteMapper.selectOne(new LambdaQueryWrapper<ChatRecordSqlite>()
                        .select(ChatRecordSqlite::getTime)
                        .eq(ChatRecordSqlite::getGroupId, message.getGroupId())
                        .eq(ChatRecordSqlite::getSelfId, message.getSelfId())
                        .eq(ChatRecordSqlite::getMessageType, MessageTypeEnum.group.getType())
                        .eq(ChatRecordSqlite::getDeleted, 0)
                        .orderByAsc(ChatRecordSqlite::getTime)
                        .last("LIMIT 1"));
                if(chatRecord != null && chatRecord.getTime() != null){
                    ForwardMsgItem instance = ForwardMsgItem.instance(message.getSelfId(), BotConfig.NAME,
                            MessageHolder.instanceText("从[" + chatRecord.getTime() + "]开始统计"));
                    forwardMsgItems.add(instance);
                }

                for (int i = 0; i < chatRecords.size(); i++) {
                    ChatRecordSqlite item = chatRecords.get(i);
                    String name = getName(item, groupMemberList);
                    String msg =(i + 1) + "\n"
                            + name + "(" +item.getUserId() + ")"
                            + "\n发言数：" + item.getTotal();

                    ForwardMsgItem instance = ForwardMsgItem.instance(item.getUserId(), name, MessageHolder.instanceText(msg));
                    forwardMsgItems.add(instance);
                }
                List<List<ForwardMsgItem>> lists = CommonUtil.averageAssignList(forwardMsgItems, 70);
                for (int i = 0; i < lists.size(); i++) {

                    bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), lists.get(i));

                    if(i < lists.size() - 1){
                        try {
                            Thread.sleep(1200);
                        }catch (InterruptedException e){}
                    }
                }
            }catch (Exception e){
                log.error("聊天统计异常",e);
                bot.sendGroupMessage(message.getGroupId(), "聊天统计异常\n"+e.getMessage(),true);
            }
            
        });
        return true;
    }

    private String getName(ChatRecordSqlite e, List<GroupMember> groupMemberList){
        if(Objects.isNull(e.getUserId()) || e.getUserId() == 0){
            return "匿名";
        }
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

        ChatRecordSqlite chatRecord = chatRecordSqliteMapper.selectOne(new LambdaQueryWrapper<ChatRecordSqlite>()
                .select(ChatRecordSqlite::getCard,ChatRecordSqlite::getNickname)
                .eq(ChatRecordSqlite::getUserId,e.getUserId())
                .eq(ChatRecordSqlite::getGroupId, e.getGroupId())
                .eq(ChatRecordSqlite::getMessageType, MessageTypeEnum.group.getType())
                .eq(ChatRecordSqlite::getDeleted, 0)
                .orderByDesc(ChatRecordSqlite::getTime)
                .last("LIMIT 1"));
        if(chatRecord != null){
            return StringUtils.isNotBlank(chatRecord.getCard()) ? chatRecord.getCard()
                    : StringUtils.isNotBlank(chatRecord.getNickname()) ? chatRecord.getNickname() : "noname";
        }
        return "noname";
    }
}
