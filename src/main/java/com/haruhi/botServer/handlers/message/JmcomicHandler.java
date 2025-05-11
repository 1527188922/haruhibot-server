package com.haruhi.botServer.handlers.message;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
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
                BaseResp<File> resp = null;
                if (isPdf) {
                    resp = jmcomicService.downloadAlbumAsPdf(finalAid);
                }else{
                    resp = jmcomicService.downloadAlbumAsZip(finalAid);
                }
                if(!BaseResp.SUCCESS_CODE.equals(resp.getCode())){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            resp.getMsg(),true);
                    return;
                }
                if(isPdf){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            MessageFormat.format("【JM{0}】下载完成,正在上传文件...\n文件密码：{1}\n也可通过浏览器打开连接进行下载\n{2}",
                                    finalAid,
                                    JmcomicService.JM_PASSWORD,
                                    webResourceConfig.webHomePath()+"/jmcomic/download/pdf/"+finalAid),true);
                    SyncResponse<String> response = null;
                    long l = System.currentTimeMillis();
                    if (MessageTypeEnum.group.getType().equals(message.getMessageType())) {
                        response = bot.uploadGroupFile(message.getGroupId(), resp.getData().getAbsolutePath(), resp.getData().getName(), null, -1);
                    }else if(MessageTypeEnum.privat.getType().equals(message.getMessageType())){
                        response = bot.uploadPrivateFile(message.getUserId(), resp.getData().getAbsolutePath(), resp.getData().getName(), -1);
                    }
                    log.info("上传本子pdf完成 cost:{} 响应：{}",(System.currentTimeMillis() - l), JSONObject.toJSONString(response));
                }else{
                    // zip 不上传qq
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            MessageFormat.format("【JM{0}】下载完成\n浏览器打开连接\n{1}",
                                    finalAid,
                                    webResourceConfig.webHomePath()+"/jmcomic/download/"+finalAid),true);
                }
            } catch (Exception e) {
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        MessageFormat.format("下载【JM{0}】异常"+e.getMessage(), finalAid),true);
            }
        });
        return true;
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
