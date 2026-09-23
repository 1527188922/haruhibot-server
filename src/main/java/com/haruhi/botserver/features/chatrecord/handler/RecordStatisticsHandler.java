package com.haruhi.botserver.features.chatrecord.handler;

import com.github.pagehelper.PageInfo;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.MessageTypeEnum;
import com.haruhi.botserver.integration.onebot.model.ForwardMsgItem;
import com.haruhi.botserver.integration.onebot.model.GroupMember;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.integration.onebot.model.SyncResponse;
import com.haruhi.botserver.features.chatrecord.model.ChatRecordVo;
import com.haruhi.botserver.bot.handler.IGroupMessageHandler;
import com.haruhi.botserver.features.chatrecord.persistence.mapper.ChatRecordGroupMapper;
import com.haruhi.botserver.features.chatrecord.service.ChatRecordService;
import com.haruhi.botserver.administration.service.SqliteDatabaseService;
import com.haruhi.botserver.shared.util.CommonUtil;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.features.chatrecord.model.ChatRecordQueryReq;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Component
@Slf4j
public class RecordStatisticsHandler implements IGroupMessageHandler {
    
    @Autowired
    private ChatRecordService chatRecordService;
    @Autowired
    private ChatRecordGroupMapper chatRecordGroupMapper;
    @Autowired
    private SqliteDatabaseService sqliteDatabaseService;

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
            Long groupId = message.getGroupId();
            Long selfId = message.getSelfId();
            try {
                String chatTableName = sqliteDatabaseService.getChatTableName(groupId, null);
                long l = System.currentTimeMillis();
                List<ChatRecordVo> recordVoList = chatRecordGroupMapper.chatStats(chatTableName, selfId);
                log.info("聊天记录分组执行sql cost:{}",System.currentTimeMillis() - l);
                if(CollectionUtils.isEmpty(recordVoList)){
                    bot.sendMessage(message.getUserId(), groupId, message.getMessageType(),
                            MessageHolder.instanceText("暂无聊天记录"));
                    return;
                }

                SyncResponse<List<GroupMember>> syncResponse = bot.getGroupMemberList(groupId, Duration.ofSeconds(12).toMillis());

                List<GroupMember> groupMemberList = syncResponse.getData();
//                if(!CollectionUtils.isEmpty(groupMemberList)){
//                    List<Long> longs = Collections.singletonList(message.getSelfId());
//                    groupMemberList.removeIf(next -> longs.contains(next.getUserId()));
//                }

                List<ForwardMsgItem> forwardMsgItems = new ArrayList<>(recordVoList.size() + 1);

                // 查询这个群最早一条消息
                ChatRecordQueryReq param = new ChatRecordQueryReq();
                param.setMessageType(MessageTypeEnum.group.getType());
                param.setGroupId(groupId);
                param.setSelfId(selfId);
                param.setPageSize(1);
                param.setSort("asc");
                PageInfo<ChatRecordVo> pageInfo = chatRecordService.search(param, true, false);
                if(pageInfo != null && !pageInfo.getList().isEmpty()){
                    ForwardMsgItem instance = ForwardMsgItem.instance(selfId, bot.getBotName(),
                            MessageHolder.instanceText("从[" + pageInfo.getList().getFirst().getTime() + "]开始统计"));
                    forwardMsgItems.add(instance);
                }

                for (int i = 0; i < recordVoList.size(); i++) {
                    ChatRecordVo item = recordVoList.get(i);
                    String name = getName(item, groupMemberList, groupId);
                    String msg =(i + 1) + "\n"
                            + name + "(" +item.getUserId() + ")"
                            + "\n发言数：" + item.getTotal();

                    ForwardMsgItem instance = ForwardMsgItem.instance(item.getUserId(), name, MessageHolder.instanceText(msg));
                    forwardMsgItems.add(instance);
                }
                List<List<ForwardMsgItem>> lists = CommonUtil.averageAssignList(forwardMsgItems, 70);
                for (int i = 0; i < lists.size(); i++) {

                    bot.sendForwardMessage(message.getUserId(), groupId, message.getMessageType(), lists.get(i));

                    if(i < lists.size() - 1){
                        try {
                            Thread.sleep(1200);
                        }catch (InterruptedException e){}
                    }
                }
            }catch (Exception e){
                log.error("聊天统计异常",e);
                bot.sendMessage(message.getUserId(), groupId,message.getMessageType(),
                        MessageHolder.instanceText("聊天统计异常\n"+e.getMessage()));
            }
            
        });
        return true;
    }

    private String getName(ChatRecordVo e, List<GroupMember> groupMemberList, Long groupId){
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

        ChatRecordQueryReq param = new ChatRecordQueryReq();
        param.setMessageType(MessageTypeEnum.group.getType());
        param.setGroupId(groupId);
        param.setUserId(e.getUserId());
        param.setPageSize(1);
        PageInfo<ChatRecordVo> pageInfo = chatRecordService.search(param, true, false);
        if (pageInfo != null && !pageInfo.getList().isEmpty()) {
            ChatRecordVo chatRecord = pageInfo.getList().getFirst();
            return StringUtils.isNotBlank(chatRecord.getCard()) ? chatRecord.getCard()
                    : StringUtils.isNotBlank(chatRecord.getNickname()) ? chatRecord.getNickname() : "noname";
        }
        return "noname";
    }
}
