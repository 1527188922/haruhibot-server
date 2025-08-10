package com.haruhi.botServer.handlers.message.chatRecord;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.annotation.SuperuserAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.qqclient.*;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.service.ChatRecordSqliteService;
import com.haruhi.botServer.utils.*;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
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
    private ChatRecordSqliteService chatRecordSqliteService;
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
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText("正在生成Excel中，请稍等..."));
            return true;
        }
        exportExcel(bot, message, lock);
        return true;
    }
   
    private void exportExcel(Bot bot, Message message, AtomicBoolean lock){
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                List<String> atQQs = message.getAtQQs();
                BaseResp<File> baseResp = chatRecordSqliteService.exportGroupChatRecord(message.getGroupId(), atQQs);
                if (!BaseResp.SUCCESS_CODE.equals(baseResp.getCode())) {
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText(baseResp.getMsg()));
                    return;
                }
                File excelFile = baseResp.getData();
                String fileName = excelFile.getName();
                String url = webResourceConfig.webExcelPath() + "/" + fileName;
                StrBuilder strBuilder = new StrBuilder();
                strBuilder.append("生成完成，正在上传群文件").append("\n");
                strBuilder.append("浏览器打开下方链接可下载Excel");

                List<ForwardMsgItem> forwardMsgs = new ArrayList<>();
                forwardMsgs.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(strBuilder.toString())));
                forwardMsgs.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(url)));

                bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgs);

                String filePath = "";
                if (!BotConfig.SAME_MACHINE_QQCLIENT) {
                    filePath = "file://" + excelFile.getAbsolutePath();
                }else{
                    long l6 = System.currentTimeMillis();
                    SyncResponse<DownloadFileResp> downloadFileRes = bot.downloadFile(url, 1, null, -1);
                    log.info("qq客户端下载群聊excel文件完成 cost:{} resp:{}",(System.currentTimeMillis()-l6),JSONObject.toJSONString(downloadFileRes));
                    if (downloadFileRes == null || downloadFileRes.getData() == null || StringUtils.isBlank(downloadFileRes.getData().getFile())) {
                        return;
                    }
                    filePath = downloadFileRes.getData().getFile();
                }
                long l5 = System.currentTimeMillis();
                log.info("开始上传qq文件：{}",filePath);
                SyncResponse<String> stringSyncResponse = bot.uploadGroupFile(message.getGroupId(), filePath, fileName, null, -1);
                log.info("群聊记录excel上传完成 cost:{} resp:{}",(System.currentTimeMillis()-l5),JSONObject.toJSONString(stringSyncResponse));
            }catch (Exception e){
                log.info("导出群聊记录异常 groupId:{}",message.getGroupId(), e);
                String err = "导出群聊记录异常\n"+ "群号："+ message.getGroupId() + "\n"+e.getMessage();
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        MessageHolder.instanceText(err));
            }finally {
                lock.set(false);
            }
        });
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText("开始生成Excel文件..."));
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
