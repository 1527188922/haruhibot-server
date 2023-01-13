package com.haruhi.botServer.service.groupChatHistory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.config.path.AbstractPathConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.dto.gocq.request.ForwardMsgItem;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.GroupChatHistory;
import com.haruhi.botServer.handlers.message.chatHistory.FindChatMessageHandler;
import com.haruhi.botServer.handlers.message.chatHistory.GroupWordCloudHandler;
import com.haruhi.botServer.mapper.GroupChatHistoryMapper;
import com.haruhi.botServer.thread.WordSlicesTask;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.WordCloudUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupChatHistoryServiceImpl extends ServiceImpl<GroupChatHistoryMapper, GroupChatHistory> implements GroupChatHistoryService{

    @Autowired
    private GroupChatHistoryMapper groupChatHistoryMapper;

    @Autowired
    private AbstractPathConfig abstractPathConfig;
    private static String path = "wordCloud";

    private static String basePath;
    public static int poolSize = SystemInfo.AVAILABLE_PROCESSORS + 1;
    private static Executor pool =  new ThreadPoolExecutor(poolSize,poolSize * 2,60L, TimeUnit.SECONDS,new ArrayBlockingQueue(8),new CustomizableThreadFactory("pool-chat-history-"),new ThreadPoolExecutor.CallerRunsPolicy());

    @PostConstruct
    private void mkdirs(){
        basePath = abstractPathConfig.resourcesImagePath() + File.separator + path;
        File file = new File(basePath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 发送聊天历史
     * 群合并类型
     * @param message
     * @param param
     */
    @Override
    public void sendChatList(WebSocketSession session,Message message, FindChatMessageHandler.Param param) {
        Date date = limitDate(param);
        LambdaQueryWrapper<GroupChatHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupChatHistory::getDeleted,false).eq(GroupChatHistory::getGroupId,message.getGroupId()).eq(GroupChatHistory::getSelfId,message.getSelfId()).gt(GroupChatHistory::getCreateTime,date.getTime());
        List<String> userIds = CommonUtil.getCqParams(message.getRawMessage(), CqCodeTypeEnum.at, "qq");
        if(!CollectionUtils.isEmpty(userIds)){
            queryWrapper.in(GroupChatHistory::getUserId,userIds);
        }
        if(FindChatMessageHandler.MessageType.IMAGE.equals(param.getMessageType())){
            // 仅查询图片类型
            queryWrapper.like(GroupChatHistory::getContent,"[CQ:image").like(GroupChatHistory::getContent,"subType=0");
        }else if(FindChatMessageHandler.MessageType.TXT.equals(param.getMessageType())){
            queryWrapper.notLike(GroupChatHistory::getContent,"[CQ:");
        }
        // 升序
        queryWrapper.orderByAsc(GroupChatHistory::getCreateTime);
        List<GroupChatHistory> chatList = groupChatHistoryMapper.selectList(queryWrapper);
        if(!CollectionUtils.isEmpty(chatList)){
            int limit = 80;
            if(chatList.size() > limit){
                // 记录条数多于80张,分开发送
                List<List<GroupChatHistory>> lists = CommonUtil.averageAssignList(chatList, limit);
                for (List<GroupChatHistory> list : lists) {
                    pool.execute(()->{
                        partSend(session,list,message);
                    });
                }
            }else{
                partSend(session,chatList,message);
            }
        }else{
            Server.sendGroupMessage(session,message.getGroupId(), "该条件下没有聊天记录。",true);
        }
    }
    private void partSend(WebSocketSession session,List<GroupChatHistory> chatList, Message message){
        List<ForwardMsgItem> params = new ArrayList<>(chatList.size());
        for (GroupChatHistory e : chatList) {
            params.add(new ForwardMsgItem(new ForwardMsgItem.Data(getName(e),e.getUserId(),e.getContent())));
        }
        Server.sendGroupMessage(session,message.getGroupId(),params);

    }
    private String getName(GroupChatHistory e){
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
    private Date limitDate(FindChatMessageHandler.Param param){
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
    public void sendWordCloudImage(WebSocketSession session, GroupWordCloudHandler.RegexEnum regexEnum, Message message) {
        // 解析查询条件
        log.info("群[{}]开始生成词云图...",message.getGroupId());
        Date date = limitDate(regexEnum);
        LambdaQueryWrapper<GroupChatHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupChatHistory::getDeleted,false).eq(GroupChatHistory::getGroupId,message.getGroupId()).eq(GroupChatHistory::getSelfId,message.getSelfId()).gt(GroupChatHistory::getCreateTime,date.getTime());
        for (GroupWordCloudHandler.RegexEnum value : GroupWordCloudHandler.RegexEnum.values()) {
            queryWrapper.notLike(GroupChatHistory::getContent,value.getRegex());
        }
        String outPutPath = null;
        List<String> userIds = CommonUtil.getCqParams(message.getRawMessage(), CqCodeTypeEnum.at, "qq");
        if (!CollectionUtils.isEmpty(userIds)) {
            queryWrapper.in(GroupChatHistory::getUserId,userIds);
        }
        // 从数据库查询聊天记录
        List<GroupChatHistory> corpus = groupChatHistoryMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(corpus)) {
            Server.sendGroupMessage(session,message.getGroupId(),"该条件下没有聊天记录",true);
            generateComplete(message);
            return;
        }

        Server.sendGroupMessage(session,message.getGroupId(),MessageFormat.format("词云图片将从{0}条聊天记录中生成,开始分词...",corpus.size()),true);
        try{
            // 开始分词
            long l = System.currentTimeMillis();
            List<String> collect = corpus.stream().map(GroupChatHistory::getContent).collect(Collectors.toList());
            List<String> strings = WordSlicesTask.execute(collect);
            // 设置权重和排除指定词语
            Map<String, Integer> map = WordCloudUtil.exclusionsWord(WordCloudUtil.setFrequency(strings));
            if(CollectionUtils.isEmpty(map)){
                Server.sendGroupMessage(session,message.getGroupId(),"分词为0，本次不生成词云图",true);
                generateComplete(message);
                return;
            }
            long l1 = System.currentTimeMillis();
            Server.sendGroupMessage(session,message.getGroupId(),MessageFormat.format("分词完成:{0}条\n耗时:{1}毫秒\n开始生成图片...",strings.size(),l1 - l),true);
            // 开始生成图片
            String fileName = regexEnum.getUnit().toString() + "-" + message.getGroupId() + ".png";
            outPutPath = basePath + File.separator + fileName;

            // 先删掉旧图片
            File file = new File(outPutPath);
            FileUtil.deleteFile(file);

            WordCloudUtil.generateWordCloudImage(map,outPutPath);
            log.info("生成词云图完成,耗时:{}",System.currentTimeMillis() - l1);
            // 生成图片完成,发送图片
            KQCodeUtils instance = KQCodeUtils.getInstance();
            String s = abstractPathConfig.webResourcesImagePath() + "/" + path + "/" + fileName + "?t=" + new Date().getTime();
            log.info("群词云图片地址：{}",s);
            String imageCq = instance.toCq(CqCodeTypeEnum.image.getType(), "file=" + s);
            //
            Server.sendGroupMessage(session,message.getGroupId(),imageCq,false);
        }catch (Exception e){
            Server.sendGroupMessage(session,message.getGroupId(),MessageFormat.format("生成词云图片异常：{0}",e.getMessage()),true);
            log.error("生成词云图片异常",e);
        }finally {
            generateComplete(message);
        }
    }
    private void generateComplete(Message message){
        GroupWordCloudHandler.lock.remove(String.valueOf(message.getGroupId()) + String.valueOf(message.getSelfId()));
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

}
