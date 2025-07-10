package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.gocq.request.ForwardMsgItem;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.ChatRecordSqlite;
import com.haruhi.botServer.handlers.message.chatRecord.FindGroupChatHandler;
import com.haruhi.botServer.handlers.message.chatRecord.GroupWordCloudHandler;
import com.haruhi.botServer.mapper.ChatRecordSqliteMapper;
import com.haruhi.botServer.thread.WordSlicesTask;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.WordCloudUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.vo.ChatRecordQueryReq;
import com.haruhi.botServer.ws.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatRecordSqliteServiceImpl extends ServiceImpl<ChatRecordSqliteMapper, ChatRecordSqlite>
        implements ChatRecordSqliteService {


    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;

    public static int poolSize = SystemInfo.AVAILABLE_PROCESSORS + 1;
    private static final Executor pool =  new ThreadPoolExecutor(poolSize,poolSize * 2,60L,
            TimeUnit.SECONDS,new ArrayBlockingQueue<>(8),
            new CustomizableThreadFactory("pool-chat-history-"),
            new ThreadPoolExecutor.CallerRunsPolicy());

    /**
     * 发送聊天历史
     * 群合并类型
     * @param message
     * @param param
     */
    @Override
    public void sendGroupChatList(Bot bot, Message message, FindGroupChatHandler.Param param) {
        Date date = limitDate(param);
        LambdaQueryWrapper<ChatRecordSqlite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatRecordSqlite::getDeleted,0)
                .eq(ChatRecordSqlite::getGroupId,message.getGroupId())
                .eq(ChatRecordSqlite::getSelfId,message.getSelfId())
                .gt(ChatRecordSqlite::getTime,date)
                .eq(ChatRecordSqlite::getMessageType, MessageTypeEnum.group.getType());
        List<String> userIds = CommonUtil.getCqParams(message.getRawMessage(), CqCodeTypeEnum.at, "qq");
        if(!CollectionUtils.isEmpty(userIds)){
            queryWrapper.in(ChatRecordSqlite::getUserId,userIds);
        }
        if(FindGroupChatHandler.MessageType.IMAGE.equals(param.getMessageType())){
            // 仅查询图片类型
            queryWrapper.like(ChatRecordSqlite::getContent,"[CQ:image").like(ChatRecordSqlite::getContent,"subType=0");
        }else if(FindGroupChatHandler.MessageType.TXT.equals(param.getMessageType())){
            queryWrapper.notLike(ChatRecordSqlite::getContent,"[CQ:");
        }
        // 升序
        queryWrapper.orderByAsc(ChatRecordSqlite::getTime);
        List<ChatRecordSqlite> chatList = this.list(queryWrapper);
        if(!CollectionUtils.isEmpty(chatList)){
            int limit = 80;
            if(chatList.size() > limit){
                // 记录条数多于80张,分开发送
                List<List<ChatRecordSqlite>> lists = CommonUtil.averageAssignList(chatList, limit);
                for (List<ChatRecordSqlite> list : lists) {
                    pool.execute(()->{
                        partSend(bot,list,message);
                    });
                }
            }else{
                partSend(bot,chatList,message);
            }
        }else{
            bot.sendGroupMessage(message.getGroupId(), "该条件下没有聊天记录。",true);
        }
    }
    private void partSend(Bot bot, List<ChatRecordSqlite> chatList, Message message){
        List<ForwardMsgItem> params = new ArrayList<>(chatList.size());
        for (ChatRecordSqlite e : chatList) {
            params.add(new ForwardMsgItem(new ForwardMsgItem.Data(getName(e),e.getUserId(),e.getContent())));
        }
        bot.sendGroupMessage(message.getGroupId(),params);

    }
    private String getName(ChatRecordSqlite e){
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

    /**
     * 发送词云图片
     * @param regexEnum
     * @param message
     */
    @Override
    public void sendWordCloudImage(Bot bot, GroupWordCloudHandler.RegexEnum regexEnum, Message message) {
        // 解析查询条件
        log.info("群[{}]开始生成词云图...",message.getGroupId());
        Date date = limitDate(regexEnum);
        LambdaQueryWrapper<ChatRecordSqlite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatRecordSqlite::getDeleted,0)
                .eq(ChatRecordSqlite::getGroupId,message.getGroupId())
                .eq(ChatRecordSqlite::getSelfId,message.getSelfId())
                .gt(ChatRecordSqlite::getTime,date)
                .eq(ChatRecordSqlite::getMessageType,MessageTypeEnum.group.getType());
        for (GroupWordCloudHandler.RegexEnum value : GroupWordCloudHandler.RegexEnum.values()) {
            queryWrapper.notLike(ChatRecordSqlite::getContent,value.getRegex());
        }
        String outPutPath = null;
//        List<String> userIds = CommonUtil.getCqParams(message.getRawMessage(), CqCodeTypeEnum.at, "qq");
        List<String> userIds = message.getAtQQs();
        queryWrapper.in(!CollectionUtils.isEmpty(userIds),ChatRecordSqlite::getUserId,userIds);
        // 从数据库查询聊天记录
        List<ChatRecordSqlite> corpus = this.baseMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(corpus)) {
            bot.sendGroupMessage(message.getGroupId(),"该条件下没有聊天记录",true);
            generateComplete(message);
            return;
        }

        bot.sendGroupMessage(message.getGroupId(), MessageFormat.format("词云图片将从{0}条聊天记录中生成,开始分词...",corpus.size()),true);
        try{
            // 开始分词
            long l = System.currentTimeMillis();
            List<String> collect = corpus.stream().map(ChatRecordSqlite::getContent).collect(Collectors.toList());
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


    @Override
    public IPage<ChatRecordSqlite> search(ChatRecordQueryReq request, boolean page) {

        LambdaQueryWrapper<ChatRecordSqlite> queryWrapper = new LambdaQueryWrapper<ChatRecordSqlite>()
                .eq(StringUtils.isNotBlank(request.getMessageType()),ChatRecordSqlite::getMessageType,request.getMessageType())
                .eq(Objects.nonNull(request.getGroupId()),ChatRecordSqlite::getGroupId,request.getGroupId())
                .eq(Objects.nonNull(request.getUserId()),ChatRecordSqlite::getUserId,request.getUserId())
                .like(StringUtils.isNotBlank(request.getContent()),ChatRecordSqlite::getContent,request.getContent())
                .like(StringUtils.isNotBlank(request.getNickName()),ChatRecordSqlite::getNickname,request.getNickName())
                .like(StringUtils.isNotBlank(request.getCard()),ChatRecordSqlite::getCard,request.getCard())
                .orderByDesc(ChatRecordSqlite::getTime);

        IPage<ChatRecordSqlite> pageInfo = null;
        if (page) {
            pageInfo = this.page(new Page<>(request.getCurrentPage(), request.getPageSize()), queryWrapper);
        }else{
            pageInfo = new Page<>(request.getCurrentPage(), request.getPageSize());
            List<ChatRecordSqlite> list = this.list(queryWrapper);
            pageInfo.setRecords(list);
            pageInfo.setTotal(list.size());
        }
        if (pageInfo != null && CollectionUtils.isNotEmpty(pageInfo.getRecords())) {
            pageInfo.getRecords().forEach(e -> {
                e.setUserAvatarUrl(CommonUtil.getAvatarUrl(e.getUserId(),false));
                e.setSelfAvatarUrl(CommonUtil.getAvatarUrl(e.getSelfId(),false));
            });
        }
        return pageInfo;
    }

}
