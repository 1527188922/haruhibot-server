package com.haruhi.botServer.handlers.message.chatRecord;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.gocq.response.DownloadFileResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.entity.ChatRecord;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.service.chatRecord.ChatRecordService;
import com.haruhi.botServer.utils.*;
import com.haruhi.botServer.utils.excel.ChatRecordExportBody;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExportGroupChatRecordHandler implements IGroupMessageEvent {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_230.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_230.getName();
    }
    
    @Autowired
    private ChatRecordService chatRecordService;
    @Autowired
    private AbstractWebResourceConfig webResourceConfig;
    
    private final static ConcurrentMap<String, AtomicBoolean> LOCK_MAP = new ConcurrentHashMap<>();


    @SuperuserAuthentication
    @Override
    public boolean onGroup(Bot bot, Message message) {
        MatchResult<Long> result = matches(message);
        if(!result.isMatched()){
            return false;
        }
        AtomicBoolean lock = LOCK_MAP.get(String.valueOf(message.getGroupId()));
        if (lock == null){
            AtomicBoolean newLock = new AtomicBoolean(false);
            LOCK_MAP.put(String.valueOf(message.getGroupId()), newLock);
            exportExcel(bot, message, newLock);
            return true;
        }
        if(!lock.compareAndSet(false,true)){
            bot.sendGroupMessage(message.getGroupId(),"正在生成Excel中，请稍等...",true);
            return true;
        }
        exportExcel(bot, message, lock);
        return true;
    }
   
    private void exportExcel(Bot bot, Message message, AtomicBoolean lock){
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            ExcelWriter excelWriter = null;
            try {
                long l = System.currentTimeMillis();
                List<String> atQQs = message.getAtQQs();
                List<ChatRecord> list = chatRecordService.list(new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getGroupId,message.getGroupId())
                        .in(!CollectionUtils.isEmpty(atQQs),ChatRecord::getUserId,atQQs.stream().map(Long::parseLong).collect(Collectors.toList()))
                        .orderByDesc(ChatRecord::getTime));
                long l4 = System.currentTimeMillis() - l;
                log.info("查询聊天记录完成，耗时：{} 数量：{}",l4, list.size());
                if(CollectionUtils.isEmpty(list)){
                    bot.sendGroupMessage(message.getGroupId(),"未查到聊天记录",true);
                    return;
                }

                File file = new File(FileUtil.getGroupChatRecordExcelFile(String.valueOf(message.getGroupId())));
                if(file.isFile()){
                    file.delete();
                }
                long l1 = System.currentTimeMillis();
                WriteSheet sheet1 = EasyExcel.writerSheet(1, "群聊记录")
                        .head(ChatRecordExportBody.class)
                        .build();
                String fileName = "group_chat_record_" + message.getGroupId()+"_"+ System.currentTimeMillis() + ".xlsx";
                FileUtil.mkdirs(FileUtil.getExcelDir());
                File file1 = new File(FileUtil.getExcelDir() + File.separator + fileName);
                if (file1.exists()) {
                    file1.delete();
                }
                excelWriter = EasyExcel.write(new FileOutputStream(file1)).build();
                excelWriter.write(convertObjToExcelData(list),sheet1);
                excelWriter.finish();
                long l2 = System.currentTimeMillis() - l;
                long l3 = System.currentTimeMillis() - l1;
                log.info("生成Excel耗时：{} 总耗时：{}",l3,l2);
                String url = webResourceConfig.webExcelPath() + "/" + fileName;
                StrBuilder strBuilder = new StrBuilder();
                strBuilder.append("生成完成，正在上传群文件\n查询聊天记录耗时：").append(l4).append("ms").append("\n");
                strBuilder.append("生成Excel耗时：").append(l3).append("ms").append("\n");
                strBuilder.append("总耗时：").append(l2).append("ms").append("\n");
                strBuilder.append("浏览器打开下方链接可下载Excel");

                ArrayList<String> forwardMsgs = new ArrayList<>();
                forwardMsgs.add(strBuilder.toString());
                forwardMsgs.add(url);
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), message.getSelfId(), BotConfig.NAME, forwardMsgs);
                long l6 = System.currentTimeMillis();
                SyncResponse<DownloadFileResp> downloadFileRes = bot.downloadFile(url, 1, null, -1);
                log.info("qq客户端下载群聊excel文件完成 cost:{} resp:{}",(System.currentTimeMillis()-l6),JSONObject.toJSONString(downloadFileRes));
                if (downloadFileRes == null || downloadFileRes.getData() == null || org.apache.commons.lang3.StringUtils.isBlank(downloadFileRes.getData().getFile())) {
                    return;
                }
                long l5 = System.currentTimeMillis();
                SyncResponse<String> stringSyncResponse = bot.uploadGroupFile(message.getGroupId(), downloadFileRes.getData().getFile(), fileName, null, -1);
                log.info("群聊记录excel上传完成 cost:{} resp:{}",(System.currentTimeMillis()-l5),JSONObject.toJSONString(stringSyncResponse));

            }catch (Exception e){
                log.info("导出群聊记录异常 groupId:{} exportGroupId:{}",message.getGroupId(),message.getGroupId(), e);
                bot.sendGroupMessage(message.getGroupId(),"导出群聊记录异常\n"+ "群号："+ message.getGroupId() + "\n"+e.getMessage(),true);
            }finally {
                lock.set(false);
                if(excelWriter != null){
                    excelWriter.finish();
                }
            }
        });
        bot.sendGroupMessage(message.getGroupId(),"开始生成Excel文件...",true);
    }

    private List<ChatRecordExportBody> convertObjToExcelData(List<ChatRecord> chatRecordList){
        List<ChatRecordExportBody> res = new ArrayList<>();
        long l = System.currentTimeMillis();
        for (ChatRecord record : chatRecordList) {
            ChatRecordExportBody exportBody = new ChatRecordExportBody();
            exportBody.setCard(record.getCard());
            exportBody.setNickName(record.getNickname());
            exportBody.setUserId(String.valueOf(record.getUserId()));
            exportBody.setContent(record.getContent());
            exportBody.setCreateTime(DateTimeUtil.dateTimeFormat(record.getTime(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
            res.add(exportBody);
        }
        log.info("转excel实体完成 耗时：{}",System.currentTimeMillis() - l);
        return res;
    }


    private MatchResult<Long> matches(Message message){
        if(!message.isTextMsg()){
            return MatchResult.unmatched();
        }
        BaseResp<String> baseResp = CommonUtil.commandStartsWith(message.getText(0).trim(), RegexEnum.EXPORT_CHAT_RECORD);
        if(!BaseResp.SUCCESS_CODE.equals(baseResp.getCode())){
            return MatchResult.unmatched();
        }
        if (StringUtils.isBlank(baseResp.getData())) {
            return MatchResult.matched(message.getGroupId());
        }
        try {
            long l = Long.parseLong(baseResp.getData());
            return MatchResult.matched(l);
        }catch (NumberFormatException e){
            return MatchResult.unmatched();
        }
    }

}
