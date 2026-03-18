package com.haruhi.botServer.service;

import com.haruhi.botServer.constant.DataBaseConst;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.Sender;
import com.haruhi.botServer.entity.ChatRecordExtendV2;
import com.haruhi.botServer.entity.ChatRecordGroup;
import com.haruhi.botServer.entity.ChatRecordPrivate;
import com.haruhi.botServer.mapper.ChatRecordExtendV2Mapper;
import com.haruhi.botServer.mapper.ChatRecordGroupMapper;
import com.haruhi.botServer.mapper.ChatRecordPrivateMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ChatRecordService implements CommandLineRunner {
    @Autowired
    private ChatRecordGroupMapper chatRecordGroupMapper;
    @Autowired
    private ChatRecordPrivateMapper chatRecordPrivateMapper;
    @Autowired
    private SqliteDatabaseService sqliteDatabaseService;
    @Autowired
    private ChatRecordExtendV2Mapper chatRecordExtendV2Mapper;


    @Override
    public void run(String... args) throws Exception {
//        for (int i = 0; i < 3; i++) {
//            ChatRecordGroup chatRecordGroup = new ChatRecordGroup();
//            chatRecordGroup.setCard("dd1d1");
//            chatRecordGroup.setNickname("dd1d1");
//            chatRecordGroup.setMessageId("1311314124");
//            chatRecordGroup.setUserId(1527188922L);
//            chatRecordGroup.setContent("ff2f23f23f23fv过了v萼绿儿");
//            chatRecordGroup.setSelfId(1527188922L);
//            chatRecordGroup.setTime(this.getTime(new Date().getTime()));
//            sqliteDatabaseService.createChatRecordGroupIfNotExists(1527188922L);
//            String tableName = DataBaseConst.T_CHAT_RECORD_GROUP_PREFIX + 1527188922L;
//            chatRecordGroupMapper.insert(tableName, chatRecordGroup);
//            System.out.println(tableName);
//            System.out.println(chatRecordGroup);
//        }
    }

    public void saveChatRecord(Message record) {
        Long userId = record.getUserId();
        if (record.isGroupMsg()) {
            Long groupId = record.getGroupId();
            String tableName = DataBaseConst.T_CHAT_RECORD_GROUP_PREFIX + groupId;
            ChatRecordGroup chatRecordGroup = new ChatRecordGroup();
            Sender sender = record.getSender();
            if (sender != null) {
                chatRecordGroup.setCard(sender.getCard());
                chatRecordGroup.setNickname(sender.getNickname());
            }
            chatRecordGroup.setMessageId(record.getMessageId());
            chatRecordGroup.setUserId(userId);
            chatRecordGroup.setContent(record.getRawMessage());
            chatRecordGroup.setSelfId(record.getSelfId());
            chatRecordGroup.setTime(this.getTime(record.getTime()));
            sqliteDatabaseService.createChatRecordGroupIfNotExists(groupId);
            chatRecordGroupMapper.insert(tableName, chatRecordGroup);
            this.saveExtendV2(chatRecordGroup.getId(), record);
            return;
        }

        String tableName = DataBaseConst.T_CHAT_RECORD_PRIVATE_PREFIX + record.getSelfId();
        ChatRecordPrivate chatRecordPrivate = new ChatRecordPrivate();
        Sender sender = record.getSender();
        if (sender != null) {
            chatRecordPrivate.setNickname(sender.getNickname());
        }
        chatRecordPrivate.setMessageId(record.getMessageId());
        chatRecordPrivate.setUserId(userId);
        chatRecordPrivate.setContent(record.getRawMessage());
        chatRecordPrivate.setTime(this.getTime(record.getTime()));
        sqliteDatabaseService.createChatRecordPrivateIfNotExists(record.getSelfId());
        chatRecordPrivateMapper.insert(tableName, chatRecordPrivate);
        this.saveExtendV2(chatRecordPrivate.getId(), record);
    }

    private void saveExtendV2(Long chatId, Message record) {
        ChatRecordExtendV2 recordExtendV2 = new ChatRecordExtendV2();
        recordExtendV2.setChatRecordId(chatId);
        recordExtendV2.setUserId(record.getUserId());
        if (record.isGroupMsg()) {
            recordExtendV2.setGroupId(record.getGroupId());
        }
        recordExtendV2.setRawWsMessage(record.getRawWsMsg());
        chatRecordExtendV2Mapper.insert(recordExtendV2);
    }



    private String getTime(Long time){
        if (time == null) {
            return DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        }
        if(String.valueOf(time).length() == 10){
            return DateTimeUtil.dateTimeFormat(new Date(time * 1000), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        }else{
            return DateTimeUtil.dateTimeFormat(time, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        }
    }

}
