package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.JmcomicService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
        aid = aid.trim();
        try {
            Long.parseLong(aid);
        }catch (NumberFormatException e) {
            return false;
        }
        final String finalAid = aid;
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                BaseResp<File> resp = jmcomicService.downloadAlbumAsZip(finalAid);
                if(!BaseResp.SUCCESS_CODE.equals(resp.getCode())){
                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                            resp.getMsg(),true);
                    return;
                }
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        MessageFormat.format("【JM{0}】下载完成\n浏览器打开连接\n{1}",
                                finalAid,
                                webResourceConfig.webHomePath()+"/jmcomic/download/"+finalAid),true);
            } catch (Exception e) {
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                        MessageFormat.format("下载【JM{0}】异常"+e.getMessage(), finalAid),true);
            }
        });
        return true;
    }


}
