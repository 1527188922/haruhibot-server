package com.haruhi.botServer.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.DataBaseConst;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.dto.qqclient.Sender;
import com.haruhi.botServer.entity.*;
import com.haruhi.botServer.entity.vo.ChatRecordVo;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.handlers.message.chatRecord.FindGroupChatHandler;
import com.haruhi.botServer.handlers.message.chatRecord.GroupWordCloudHandler;
import com.haruhi.botServer.mapper.ChatRecordExtendSqliteMapper;
import com.haruhi.botServer.mapper.ChatRecordExtendV2Mapper;
import com.haruhi.botServer.mapper.ChatRecordGroupMapper;
import com.haruhi.botServer.mapper.ChatRecordPrivateMapper;
import com.haruhi.botServer.thread.WordSlicesTask;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.WordCloudUtil;
import com.haruhi.botServer.utils.excel.ChatRecordExportBody;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import com.haruhi.botServer.ws.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;


    @Autowired
    private ChatRecordSqliteService chatRecordSqliteService;
    @Autowired
    private ChatRecordExtendSqliteMapper chatRecordExtendSqliteMapper;
    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;

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

    public PageInfo groupChat2Vo(PageInfo pageInfo, Long groupId){
        List<ChatRecordGroup> records = pageInfo.getList();
        if (CollectionUtils.isEmpty(records)) {
            return pageInfo;
        }

        Map<Long, List<GroupInfoSqlite>> groupMap = groupInfoSqliteService.selectMapByGroupIds(Collections.singletonList(groupId));

        List<ChatRecordVo> list = records.stream().map(r -> {
            ChatRecordVo e = new ChatRecordVo();
            BeanUtils.copyProperties(r, e);

            e.setUserAvatarUrl(CommonUtil.getAvatarUrl(e.getUserId(), false));
            e.setSelfAvatarUrl(CommonUtil.getAvatarUrl(e.getSelfId(), false));
            e.setGroupId(groupId);
            e.setMessageType(MessageTypeEnum.group.getType());
            List<GroupInfoSqlite> groupInfo = groupMap.get(groupId);
            if (groupInfo != null) {
                e.setGroupName(groupInfo.getFirst().getGroupName());
            }
            return e;
        }).toList();
        pageInfo.setList(list);
        return pageInfo;
    }

    public PageInfo privateChat2Vo(PageInfo pageInfo, Long selfId){
        List<ChatRecordPrivate> records = pageInfo.getList();
        if (CollectionUtils.isEmpty(records)) {
            return pageInfo;
        }

        List<ChatRecordVo> list = records.stream().map(r -> {
            ChatRecordVo e = new ChatRecordVo();
            BeanUtils.copyProperties(r, e);

            e.setSelfId(selfId);
            e.setUserAvatarUrl(CommonUtil.getAvatarUrl(e.getUserId(), false));
            e.setSelfAvatarUrl(CommonUtil.getAvatarUrl(selfId, false));
            e.setMessageType(MessageTypeEnum.privat.getType());
            return e;
        }).toList();
        pageInfo.setList(list);
        return pageInfo;
    }

    public PageInfo search(ChatRecordQueryReq request, boolean page, boolean needCount) {
        if (MessageTypeEnum.group.getType().equals(request.getMessageType())) {
            String chatTableName = sqliteDatabaseService.getChatTableName(request.getGroupId(), null);
            boolean b = sqliteDatabaseService.checkTableExists(chatTableName);
            if (!b){
                throw new BusinessException("Table不存在："+chatTableName);
            }

            return this.groupChat2Vo(this.groupSearch(request, chatTableName, page, needCount), request.getGroupId());
        }
        if (MessageTypeEnum.privat.getType().equals(request.getMessageType())) {
            String chatTableName = sqliteDatabaseService.getChatTableName(null, request.getSelfId());
            boolean b = sqliteDatabaseService.checkTableExists(chatTableName);
            if (!b){
                throw new BusinessException("Table不存在："+chatTableName);
            }
            return this.privateChat2Vo(this.privateSearch(request, chatTableName, page, needCount), request.getSelfId());
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


    public void sendGroupChatList(Bot bot, Message message, FindGroupChatHandler.Param param) {
        String startTime = DateTimeUtil.dateTimeFormat(limitDate(param), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);

        ChatRecordQueryReq req = new ChatRecordQueryReq();
        req.setMessageType(MessageTypeEnum.group.getType());
        req.setGroupId(message.getGroupId());
        req.setSelfId(message.getSelfId());
        req.setStartTime(startTime);
        req.setSort("asc");

        List<String> userIds = CommonUtil.getCqParams(message.getRawMessage(), CqCodeTypeEnum.at, "qq");
        if(!CollectionUtils.isEmpty(userIds)){
            req.setUserIds(userIds.stream().map(Long::parseLong).toList());
        }
//        if(FindGroupChatHandler.MessageType.IMAGE.equals(param.getMessageType())){
//            // 仅查询图片类型
//            queryWrapper.like(ChatRecordSqlite::getContent,"[CQ:image").like(ChatRecordSqlite::getContent,"subType=0");
//        }else if(FindGroupChatHandler.MessageType.TXT.equals(param.getMessageType())){
//            queryWrapper.notLike(ChatRecordSqlite::getContent,"[CQ:");
//        }
        // 升序
        PageInfo<ChatRecordGroup> pageInfo = this.search(req, false, false);
        List<ChatRecordGroup> chatList = pageInfo.getList();
        if(CollectionUtils.isEmpty(chatList)){
            bot.sendGroupMessage(message.getGroupId(), "该条件下没有聊天记录。",true);
            return;
        }
        int limit = 80;
        if(chatList.size() > limit){
            // 记录条数多于80张,分开发送
            List<List<ChatRecordGroup>> lists = CommonUtil.averageAssignList(chatList, limit);
            lists.forEach(list -> {
                partSend(bot,list,message);
            });
        }else{
            partSend(bot,chatList,message);
        }
    }
    private void partSend(Bot bot, List<ChatRecordGroup> chatList, Message message){
        List<ForwardMsgItem> forwardMsgItems = new ArrayList<>(chatList.size());
        for (ChatRecordGroup e : chatList) {
            ForwardMsgItem instance = ForwardMsgItem.instance(e.getUserId(), getName(e), MessageHolder.instanceText(e.getContent()));
            forwardMsgItems.add(instance);
        }
        bot.sendForwardMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),forwardMsgItems);

    }
    private String getName(ChatRecordGroup e){
        try {
            if(Strings.isNotBlank(e.getCard().trim())){
                return e.getCard();
            }
            if(Strings.isNotBlank(e.getNickname().trim())){
                return e.getNickname();
            }
        }catch (Exception ex){
        }
        return "noname";
    }
    private Date limitDate(FindGroupChatHandler.Param param){
        Date res = null;
        Date current = new Date();
        switch (param.getUnit()){
            case DAY:
                if (param.getNum() > 15) {
                    param.setNum(15);
                }
                res = DateTimeUtil.addDay(current,-(param.getNum()));
                break;
            case HOUR:
                int limit = 15 * 24;
                if (param.getNum() > limit) {
                    param.setNum(limit);
                }
                res = DateTimeUtil.addHour(current,-(param.getNum()));
                break;
            default:
                break;
        }
        return res;
    }


    public void sendWordCloudImage(Bot bot, GroupWordCloudHandler.RegexEnum regexEnum, Message message) {
        // 解析查询条件
        log.info("群[{}]开始生成词云图...",message.getGroupId());
        String date = DateTimeUtil.dateTimeFormat(limitDate(regexEnum), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
        String outPutPath = null;
//        List<String> userIds = CommonUtil.getCqParams(message.getRawMessage(), CqCodeTypeEnum.at, "qq");
        List<String> userIds = message.getAtQQs();
        // 从数据库查询聊天记录
        String chatTableName = sqliteDatabaseService.getChatTableName(message.getGroupId(), null);
        List<Long> longs = CollectionUtils.isNotEmpty(userIds) ? userIds.stream().map(Long::parseLong).toList() : null;
        List<String> list = Arrays.stream(GroupWordCloudHandler.RegexEnum.values()).map(GroupWordCloudHandler.RegexEnum::getRegex).toList();

        List<ChatRecordGroup> corpus = chatRecordGroupMapper.selectWordCloudCorpus(chatTableName, message.getSelfId(), longs,date ,list);
        if (CollectionUtils.isEmpty(corpus)) {
            bot.sendGroupMessage(message.getGroupId(),"该条件下没有聊天记录",true);
            generateComplete(message);
            return;
        }

        bot.sendGroupMessage(message.getGroupId(), MessageFormat.format("词云图片将从{0}条聊天记录中生成,开始分词...",corpus.size()),true);
        try{
            // 开始分词
            long l = System.currentTimeMillis();
            List<String> collect = corpus.stream().map(ChatRecordGroup::getContent).collect(Collectors.toList());
            List<String> strings = WordSlicesTask.execute(collect);
            // 设置权重和排除指定词语
            Map<String, Integer> map = WordCloudUtil.exclusionsWord(WordCloudUtil.setFrequency(strings));
            if(org.springframework.util.CollectionUtils.isEmpty(map)){
                bot.sendGroupMessage(message.getGroupId(),"分词为0，本次不生成词云图",true);
                generateComplete(message);
                return;
            }
            long l1 = System.currentTimeMillis();
            bot.sendGroupMessage(message.getGroupId(),MessageFormat.format("分词完成:{0}条\n耗时:{1}毫秒\n开始生成图片...",strings.size(),l1 - l),true);
            // 开始生成图片
            String fileName = regexEnum.getUnit().toString() + "-" + message.getGroupId() + ".png";
            outPutPath = FileUtil.mkdirs(FileUtil.getWordCloudDir()) + File.separator + fileName;

            // 先删掉旧图片
            File file = new File(outPutPath);
            FileUtil.deleteFile(file);

            WordCloudUtil.generateWordCloudImage(map,outPutPath);
            log.info("生成词云图完成,耗时:{}",System.currentTimeMillis() - l1);
            // 生成图片完成,发送图片
            KQCodeUtils instance = KQCodeUtils.getInstance();
            String s = abstractPathConfig.webWordCloudPath() + "/" + fileName + "?t=" + System.currentTimeMillis();
            log.info("群词云图片地址：{}",s);
            String imageCq = instance.toCq(CqCodeTypeEnum.image.getType(), "file=" + s);
            bot.sendGroupMessage(message.getGroupId(),imageCq,false);
        }catch (Exception e){
            bot.sendGroupMessage(message.getGroupId(),MessageFormat.format("生成词云图片异常：{0}",e.getMessage()),true);
            log.error("生成词云图片异常",e);
        }finally {
            generateComplete(message);
        }
    }
    private void generateComplete(Message message){
        GroupWordCloudHandler.lock.remove(String.valueOf(message.getGroupId()) + message.getSelfId());
    }

    private Date limitDate(GroupWordCloudHandler.RegexEnum regexEnum){
        Date res = null;
        Date current = new Date();
        switch (regexEnum.getUnit()){
            case YEAR:
                res = DateTimeUtil.formatToDate(current, DateTimeUtil.PatternEnum.yyyy);
                break;
            case MONTH:
                res = DateTimeUtil.formatToDate(current, DateTimeUtil.PatternEnum.yyyyMM);
                break;
            case WEEK:
                // 获取本周第一天日期
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(current);
                calendar.set(Calendar.DAY_OF_WEEK, 2);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                res = calendar.getTime();
                break;
            case DAY:
                res = DateTimeUtil.formatToDate(current, DateTimeUtil.PatternEnum.yyyyMMdd);
                break;
            default:
                break;
        }
        return res;
    }
    public void migratePrivateData(Long selfId){
        int pageSize = 1000;
        int currentPage = 1;
        LambdaQueryWrapper<ChatRecordSqlite> eq = new LambdaQueryWrapper<ChatRecordSqlite>()
                .eq(ChatRecordSqlite::getMessageType, MessageTypeEnum.privat.getType())
                .eq(ChatRecordSqlite::getSelfId, selfId);
        while (true){
            PageInfo<ChatRecordSqlite> pageinfo = PageHelper.startPage(currentPage, pageSize, false).doSelectPageInfo(() -> {
                chatRecordSqliteService.list(eq);
            });
            if (CollectionUtils.isEmpty(pageinfo.getList())) {
                log.info("执行完成：{}",selfId);
                break;
            }

            List<ChatRecordSqlite> records = pageinfo.getList();
            List<MutablePair<ChatRecordPrivate, Long>> list = records.stream().map(e -> {
                ChatRecordPrivate chatRecordPrivate = new ChatRecordPrivate();
                chatRecordPrivate.setTime(e.getTime());
                chatRecordPrivate.setContent(e.getContent());
                chatRecordPrivate.setUserId(e.getUserId());
                chatRecordPrivate.setNickname(e.getNickname());
                chatRecordPrivate.setDeleted(e.getDeleted());
                chatRecordPrivate.setMessageId(e.getMessageId());
                return MutablePair.of(chatRecordPrivate, e.getId());
            }).toList();
            List<Long> list1 = records.stream().map(ChatRecordSqlite::getId).toList();
            List<ChatRecordExtendSqlite> chatRecordExtendSqlites = chatRecordExtendSqliteMapper.selectList(new LambdaQueryWrapper<ChatRecordExtendSqlite>()
                    .in(ChatRecordExtendSqlite::getChatRecordId, list1));

            for (MutablePair<ChatRecordPrivate, Long> mutablePair : list) {
                Long oldId = mutablePair.getRight();
                ChatRecordPrivate left = mutablePair.getLeft();

                String chatTableName = sqliteDatabaseService.getChatTableName(null, selfId);
                sqliteDatabaseService.createChatRecordPrivateIfNotExists(selfId);
                chatRecordPrivateMapper.insert(chatTableName, left);

                chatRecordExtendSqlites.stream().filter(ex -> oldId.equals(ex.getChatRecordId())).findFirst().ifPresent(ex -> {
                    ChatRecordExtendV2 recordExtendV2 = new ChatRecordExtendV2();
                    recordExtendV2.setChatRecordId(left.getId());
                    recordExtendV2.setUserId(left.getUserId());
                    recordExtendV2.setRawWsMessage(ex.getRawWsMessage());
                    chatRecordExtendV2Mapper.insert(recordExtendV2);
                });
            }
            currentPage++;
        }
    }

    public void migrateGroupData(Long groupId){
        int pageSize = 1000;
        int currentPage = 1;
        while (true){
            LambdaQueryWrapper<ChatRecordSqlite> eq = new LambdaQueryWrapper<ChatRecordSqlite>()
                    .eq(ChatRecordSqlite::getGroupId, groupId);
            PageInfo<ChatRecordSqlite> pageinfo = PageHelper.startPage(currentPage, pageSize, false).doSelectPageInfo(() -> {
                chatRecordSqliteService.list(eq);
            });

            if (CollectionUtils.isEmpty(pageinfo.getList())) {
                log.info("执行完成：{}",groupId);
                break;
            }

            List<ChatRecordSqlite> records = pageinfo.getList();
            List<MutablePair<ChatRecordGroup, Long>> list = records.stream().map(e -> {
                ChatRecordGroup chatRecordGroup = new ChatRecordGroup();
                chatRecordGroup.setTime(e.getTime());
                chatRecordGroup.setContent(e.getContent());
                chatRecordGroup.setUserId(e.getUserId());
                chatRecordGroup.setSelfId(e.getSelfId());
                chatRecordGroup.setCard(e.getCard());
                chatRecordGroup.setNickname(e.getNickname());
                chatRecordGroup.setDeleted(e.getDeleted());
                chatRecordGroup.setMessageId(e.getMessageId());
                return MutablePair.of(chatRecordGroup, e.getId());
            }).toList();
            List<Long> list1 = records.stream().map(ChatRecordSqlite::getId).toList();
            List<ChatRecordExtendSqlite> chatRecordExtendSqlites = chatRecordExtendSqliteMapper.selectList(new LambdaQueryWrapper<ChatRecordExtendSqlite>()
                    .in(ChatRecordExtendSqlite::getChatRecordId, list1));

            for (MutablePair<ChatRecordGroup, Long> mutablePair : list) {
                Long oldId = mutablePair.getRight();
                ChatRecordGroup left = mutablePair.getLeft();

                String chatTableName = sqliteDatabaseService.getChatTableName(groupId, null);
                sqliteDatabaseService.createChatRecordGroupIfNotExists(groupId);
                chatRecordGroupMapper.insert(chatTableName, left);

                chatRecordExtendSqlites.stream().filter(ex -> oldId.equals(ex.getChatRecordId())).findFirst().ifPresent(ex -> {
                    ChatRecordExtendV2 recordExtendV2 = new ChatRecordExtendV2();
                    recordExtendV2.setChatRecordId(left.getId());
                    recordExtendV2.setUserId(left.getUserId());
                    recordExtendV2.setGroupId(groupId);
                    recordExtendV2.setRawWsMessage(ex.getRawWsMessage());
                    chatRecordExtendV2Mapper.insert(recordExtendV2);
                });
            }

            currentPage++;
        }
    }


}
