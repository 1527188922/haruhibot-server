package com.haruhi.botServer.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.haruhi.botServer.constant.DataBaseConst;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.Sender;
import com.haruhi.botServer.entity.ChatRecordExtendV2;
import com.haruhi.botServer.entity.ChatRecordGroup;
import com.haruhi.botServer.entity.ChatRecordPrivate;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.mapper.ChatRecordExtendV2Mapper;
import com.haruhi.botServer.mapper.ChatRecordGroupMapper;
import com.haruhi.botServer.mapper.ChatRecordPrivateMapper;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.excel.ChatRecordExportBody;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
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

    public PageInfo search(ChatRecordQueryReq request, boolean page, boolean needCount) {
        if (MessageTypeEnum.group.getType().equals(request.getMessageType())) {
            String chatTableName = sqliteDatabaseService.getChatTableName(request.getGroupId(), null);
            boolean b = sqliteDatabaseService.checkTableExists(chatTableName);
            if (!b){
                throw new BusinessException("Table不存在："+chatTableName);
            }

            return this.groupSearch(request, chatTableName, page, needCount);
        }
        if (MessageTypeEnum.privat.getType().equals(request.getMessageType())) {
            String chatTableName = sqliteDatabaseService.getChatTableName(null, request.getSelfId());
            boolean b = sqliteDatabaseService.checkTableExists(chatTableName);
            if (!b){
                throw new BusinessException("Table不存在："+chatTableName);
            }
            return this.privateSearch(request, chatTableName, page, needCount);
        }
        throw new BusinessException("查询消息错误："+request.getMessageType());
    }

    public PageInfo groupSearch(ChatRecordQueryReq request, String tableName, boolean page, boolean needCount) {
        if (page) {
            return PageHelper.startPage(request.getCurrentPage(),request.getPageSize(), needCount).<ChatRecordGroup>doSelectPageInfo(() -> {
                chatRecordGroupMapper.selectList(tableName, request);
            });
        }
        List<ChatRecordGroup> chatRecordGroups = chatRecordGroupMapper.selectList(tableName, request);
        return new PageInfo<>(chatRecordGroups);
    }

    public PageInfo privateSearch(ChatRecordQueryReq request, String tableName, boolean page, boolean needCount) {
        if (page) {
            return PageHelper.startPage(request.getCurrentPage(),request.getPageSize(), needCount).doSelectPageInfo(() -> {
                chatRecordPrivateMapper.selectList(tableName, request);
            });
        }
        List<ChatRecordPrivate> chatRecordPrivates = chatRecordPrivateMapper.selectList(tableName, request);
        return new PageInfo<>(chatRecordPrivates);
    }


    /**
     * 导出群聊天记录
     * @param groupId
     * @param qqs
     * @return
     */
    public BaseResp<File> exportGroupChatRecord(Long groupId, List<String> qqs) {
        long l = System.currentTimeMillis();
        ChatRecordQueryReq param = new ChatRecordQueryReq();
        param.setMessageType(MessageTypeEnum.group.getType());
        param.setGroupId(groupId);
        param.setUserIds(CollectionUtils.isNotEmpty(qqs) ? qqs.stream()
                .filter(StringUtils::isNotBlank)
                .distinct()
                .map(Long::parseLong)
                .toList() : null);
        PageInfo<ChatRecordGroup> pageInfo = this.search(param, false, false);
        long l4 = System.currentTimeMillis() - l;
        List<ChatRecordGroup> list = pageInfo.getList();
        log.info("查询聊天记录完成，耗时：{} 数量：{}",l4, list.size());
        if(CollectionUtils.isEmpty(list)){
            return BaseResp.fail("未查到聊天记录");
        }

        long l1 = System.currentTimeMillis();
        WriteSheet sheet1 = EasyExcel.writerSheet(1, "群聊记录")
                .head(ChatRecordExportBody.class)
                .build();
        String fileName = "group_chat_record_" + groupId+"_"+ System.currentTimeMillis() + ".xlsx";
        FileUtil.mkdirs(FileUtil.getExcelDir());
        File file = new File(FileUtil.getExcelDir() + File.separator + fileName);
        if (file.exists()) {
            file.delete();
        }
        ExcelWriter excelWriter = null;
        OutputStream outputStream = null;
        try {
            outputStream = Files.newOutputStream(file.toPath());
            excelWriter = EasyExcel.write(outputStream).build();
            excelWriter.write(convertObjToExcelData(list),sheet1);
            excelWriter.finish();
            long l2 = System.currentTimeMillis() - l;
            long l3 = System.currentTimeMillis() - l1;
            log.info("生成Excel耗时：{} 总耗时：{}",l3,l2);
            return BaseResp.success(file);
        }catch (Exception e){
            log.error("生成群聊记录excel异常",e);
            return BaseResp.fail(e.getMessage());
        }finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) { }
            }
            if (excelWriter != null) {
                try {
                    excelWriter.finish();
                }catch (Exception e){  }
            }
        }
    }

    private List<ChatRecordExportBody> convertObjToExcelData(List<ChatRecordGroup> chatRecordList){
        return chatRecordList.stream().map(record -> {
            ChatRecordExportBody exportBody = new ChatRecordExportBody();
            exportBody.setCard(record.getCard());
            exportBody.setNickName(record.getNickname());
            exportBody.setUserId(String.valueOf(record.getUserId()));
            exportBody.setContent(record.getContent());
            exportBody.setCreateTime(record.getTime());
            return exportBody;
        }).toList();
    }

}
