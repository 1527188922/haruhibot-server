package com.haruhi.botServer.handlers.message.chatRecord;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.ChatRecord;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.service.chatRecord.ChatRecordService;
import com.haruhi.botServer.utils.*;
import com.haruhi.botServer.utils.excel.ChatRecordExportBody;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
        AtomicBoolean lock = LOCK_MAP.get(String.valueOf(result.getData()));
        if (lock == null){
            AtomicBoolean newLock = new AtomicBoolean(false);
            LOCK_MAP.put(String.valueOf(result.getData()), newLock);
            exportExcel(bot, message,result.getData(), newLock);
            return true;
        }
        if(!lock.compareAndSet(false,true)){
            bot.sendGroupMessage(message.getGroupId(),"正在生成Excel中，请稍等...",true);
            return true;
        }
        exportExcel(bot, message,result.getData(), lock);
        return true;
    }
   
    private void exportExcel(Bot bot, Message message,Long exportGroupId, AtomicBoolean lock){
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            ExcelWriter excelWriter = null;
            try {
                long l = System.currentTimeMillis();
                List<ChatRecord> list = chatRecordService.list(new LambdaQueryWrapper<ChatRecord>()
                        .eq(ChatRecord::getGroupId,exportGroupId)
                        .orderByDesc(ChatRecord::getTime));
                long l4 = System.currentTimeMillis() - l;
                log.info("查询聊天记录完成，耗时：{} 数量：{}",l4, list.size());
                if(CollectionUtils.isEmpty(list)){
                    bot.sendGroupMessage(message.getGroupId(),"未查到群："+exportGroupId+"的聊天记录",true);
                    return;
                }

                File file = new File(FileUtil.getGroupChatRecordExcelFile(String.valueOf(exportGroupId)));
                if(file.isFile()){
                    file.delete();
                }
                long l1 = System.currentTimeMillis();
                WriteSheet sheet1 = EasyExcel.writerSheet(1, "群聊记录")
                        .head(ChatRecordExportBody.class)
                        .build();
                String fileName = "group_chat_record_" + exportGroupId + ".xlsx";
                excelWriter = EasyExcel.write(new FileOutputStream(FileUtil.mkdirs(FileUtil.getExcelDir()) + File.separator + fileName)).build();
                excelWriter.write(convertObjToExcelData(list),sheet1);
                long l2 = System.currentTimeMillis() - l;
                long l3 = System.currentTimeMillis() - l1;
                log.info("生成Excel耗时：{} 总耗时：{}",l3,l2);
                StrBuilder strBuilder = new StrBuilder();
                strBuilder.append("生成完成\n查询聊天记录耗时：").append(l4).append("ms").append("\n");
                strBuilder.append("生成Excel耗时：").append(l3).append("ms").append("\n");
                strBuilder.append("总耗时：").append(l2).append("ms").append("\n");
                strBuilder.append("浏览器打开下方链接可下载Excel：\n");
                strBuilder.append(webResourceConfig.webExcelPath() + "/" + fileName);
                bot.sendGroupMessage(message.getGroupId(), strBuilder.toString(), true);
            }catch (Exception e){
                log.info("导出群聊记录异常 groupId:{} exportGroupId:{}",message.getGroupId(),exportGroupId, e);
                bot.sendGroupMessage(message.getGroupId(),"导出群聊记录异常\n"+ "群号："+ exportGroupId + "\n"+e.getMessage(),true);
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
        if(!message.isTextMsg() || (message.isAtMsg() && !message.isAtBot())){
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
