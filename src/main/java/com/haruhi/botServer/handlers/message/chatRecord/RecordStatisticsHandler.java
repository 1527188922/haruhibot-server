package com.haruhi.botServer.handlers.message.chatRecord;

import com.github.pagehelper.PageInfo;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.qqclient.*;
import com.haruhi.botServer.entity.ChatRecordGroup;
import com.haruhi.botServer.entity.vo.ChatRecordVo;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.mapper.ChatRecordGroupMapper;
import com.haruhi.botServer.service.ChatRecordService;
import com.haruhi.botServer.service.SqliteDatabaseService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
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
            try {
                String chatTableName = sqliteDatabaseService.getChatTableName(message.getGroupId(), null);
                long l = System.currentTimeMillis();
                List<ChatRecordVo> recordVoList = chatRecordGroupMapper.chatStats(chatTableName, message.getSelfId());
                log.info("聊天记录分组执行sql cost:{}",System.currentTimeMillis() - l);
                if(CollectionUtils.isEmpty(recordVoList)){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            MessageHolder.instanceText("暂无聊天记录"));
                    return;
                }

                SyncResponse<List<GroupMember>> syncResponse = bot.getGroupMemberList(message.getGroupId(), 10 * 1000);

                List<GroupMember> groupMemberList = syncResponse.getData();
//                if(!CollectionUtils.isEmpty(groupMemberList)){
//                    List<Long> longs = Collections.singletonList(message.getSelfId());
//                    groupMemberList.removeIf(next -> longs.contains(next.getUserId()));
//                }

                List<ForwardMsgItem> forwardMsgItems = new ArrayList<>(recordVoList.size() + 1);

                ChatRecordQueryReq param = new ChatRecordQueryReq();
                param.setMessageType(MessageTypeEnum.group.getType());
                param.setGroupId(message.getGroupId());
                param.setUserId(message.getUserId());
                param.setPageSize(1);
                param.setSort("asc");
                PageInfo<ChatRecordVo> pageInfo = chatRecordService.search(param, true, false);
                if(pageInfo != null && !pageInfo.getList().isEmpty()){
                    ForwardMsgItem instance = ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(),
                            MessageHolder.instanceText("从[" + pageInfo.getList().getFirst().getTime() + "]开始统计"));
                    forwardMsgItems.add(instance);
                }

                for (int i = 0; i < recordVoList.size(); i++) {
                    ChatRecordVo item = recordVoList.get(i);
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
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        MessageHolder.instanceText("聊天统计异常\n"+e.getMessage()));
            }
            
        });
        return true;
    }

    private String getName(ChatRecordVo e, List<GroupMember> groupMemberList){
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
        param.setGroupId(e.getGroupId());
        param.setUserId(e.getUserId());
        param.setPageSize(1);
        PageInfo<ChatRecordVo> pageInfo = chatRecordService.search(param, true, false);
        if (!pageInfo.getList().isEmpty()) {
            ChatRecordVo chatRecord = pageInfo.getList().getFirst();
            return StringUtils.isNotBlank(chatRecord.getCard()) ? chatRecord.getCard()
                    : StringUtils.isNotBlank(chatRecord.getNickname()) ? chatRecord.getNickname() : "noname";
        }
        return "noname";
    }
}
