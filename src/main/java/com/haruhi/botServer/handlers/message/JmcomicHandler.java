package com.haruhi.botServer.handlers.message;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.qqclient.DownloadFileResp;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.JmcomicService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;

@Service
@Slf4j
public class JmcomicHandler implements IAllMessageEvent {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_570.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_570.getName();
    }

    @Autowired
    private JmcomicService jmcomicService;
    @Autowired
    private AbstractWebResourceConfig webResourceConfig;

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if(!message.isTextMsgOnly()){
            return false;
        }
        String aid = CommonUtil.commandReplaceFirst(message.getText(0), RegexEnum.JM_COMIC_DOWNLOAD);
        if(StringUtils.isBlank(aid)){
            return false;
        }
        Pair<String,Boolean> pair = calcAid(aid);
        if(pair == null){
            return false;
        }
        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                MessageFormat.format("开始下载【JM{0}】", pair.getKey()),true);
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            String finalAid = pair.getKey();
            boolean isPdf = pair.getRight();
            try {
                BaseResp<File> resp = isPdf ? jmcomicService.downloadAlbumAsPdf(finalAid) : jmcomicService.downloadAlbumAsZip(finalAid);
                if(!BaseResp.SUCCESS_CODE.equals(resp.getCode())){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            resp.getMsg(),true);
                    return;
                }

                ArrayList<String> forwardMsgs = new ArrayList<>();
                forwardMsgs.add(MessageFormat.format("【JM{0}】下载完成,正在上传QQ文件...\n也可通过浏览器打开下方链接进行下载", finalAid));
                String fileUrl = isPdf ? webResourceConfig.webHomePath()+"/jmcomic/download/pdf/"+finalAid
                        : webResourceConfig.webHomePath()+"/jmcomic/download/"+finalAid;
                forwardMsgs.add(fileUrl);
                forwardMsgs.add(isPdf ? "PDF保护密码："+JmcomicService.JM_PASSWORD : "ZIP解压密码："+JmcomicService.JM_PASSWORD);

                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), message.getSelfId(), BotConfig.NAME, forwardMsgs);
                uploadFile(bot, message, resp.getData(),fileUrl,isPdf);
            } catch (Exception e) {
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        MessageFormat.format("下载【JM{0}】异常"+e.getMessage(), finalAid),true);
            }
        });
        return true;
    }

    private void uploadFile(Bot bot,Message message,File file, String fileUrl, boolean isPdf){
        String absolutePath = null;
        if (BotConfig.SAME_MACHINE_QQCLIENT) {
            absolutePath = file.getAbsolutePath();
        }else{
            log.info("qq客户端开始下载文件：{}",fileUrl);
            long l1 = System.currentTimeMillis();
            SyncResponse<DownloadFileResp> downloadFileRes = bot.downloadFile(fileUrl, 1, null, -1);
            log.info("qq客户端下载文件完成 cost:{} resp:{}",(System.currentTimeMillis() - l1),JSONObject.toJSONString(downloadFileRes));
            if (downloadFileRes == null || downloadFileRes.getData() == null || StringUtils.isBlank(downloadFileRes.getData().getFile())) {
                return;
            }
            absolutePath = downloadFileRes.getData().getFile();
        }
        SyncResponse<String> response = null;
        log.info("qq客户端开始上传文件 {}",absolutePath);
        long l = System.currentTimeMillis();
        if (MessageTypeEnum.group.getType().equals(message.getMessageType())) {
            response = bot.uploadGroupFile(message.getGroupId(), absolutePath, file.getName(), null, -1);
        }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
            response = bot.uploadPrivateFile(message.getUserId(), absolutePath, file.getName(), -1);
        }
        log.info(isPdf ? "上传本子pdf完成 cost:{} 响应：{}" : "上传本子zip完成 cost:{} 响应：{}"
                ,(System.currentTimeMillis() - l), JSONObject.toJSONString(response));
    }

    private Pair<String,Boolean> calcAid(String aid){
        aid = aid.trim();
        try {
            Long.parseLong(aid);
            return Pair.of(aid, true);
        }catch (NumberFormatException e) {
            if (!aid.toLowerCase().endsWith("zip")) {
                return null;
            }
            aid = CommonUtil.replaceIgnoreCase(aid, "zip", "");
            try {
                Long.parseLong(aid);
                return Pair.of(aid, false);
            }catch (NumberFormatException ex) {
                return null;
            }
        }
    }
}
