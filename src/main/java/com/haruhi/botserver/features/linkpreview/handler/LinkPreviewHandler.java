package com.haruhi.botserver.features.linkpreview.handler;

import com.haruhi.botserver.bot.handler.IPrivateMessageHandler;

import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.configuration.metadata.ConfigKey;

import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.features.torrent.client.model.whatslink.AnalysisMagnetLinkResp;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.shared.util.CommonUtil;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LinkPreviewHandler implements IPrivateMessageHandler {


    @Override
    public int weight() {
        return HandlerWeightEnum.W_210.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_210.getName();
    }


    @Override
    public boolean onPrivate(Bot bot, Message message) {
        if(!message.isTextMsgOnly() || !CommonUtil.isValidMagnetLink(message.getText(-1))){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            String link = message.getText(-1);
            AnalysisMagnetLinkResp resp = request(link);
            if(resp == null || resp.getCount() == null || resp.getCount() == 0){
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText("磁力未解析出结果\n"+link));
                return;
            }
            if(StringUtils.isNotBlank(resp.getError())){
                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText("磁力解析异常\n"+resp.getError()));
                return;
            }
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),MessageHolder.instanceText(formatterResp(resp)));
        });
        return true;
    }

    private String formatterResp(AnalysisMagnetLinkResp resp){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(resp.getName()).append("\n")
                .append("类型：").append(resp.getFileType()).append("\n")
                .append("文件数量：").append(resp.getCount());
        if(resp.getSize() != null){
            String size = DataSizeUtil.format(resp.getSize());
            stringBuilder.append("\n").append("总大小：").append(size);
        }
        if (!CollectionUtils.isEmpty(resp.getScreenshots())) {
            List<String> collect = resp.getScreenshots().stream().map(AnalysisMagnetLinkResp.Screenshots::getScreenshot).collect(Collectors.toList());
            String join = StringUtils.join(collect, "\n");
            stringBuilder.append("\n预览图链接：\n").append(join);
        }
        return stringBuilder.toString();
    }

    private AnalysisMagnetLinkResp request(String link){
        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("url",link);
        String s = HttpUtil.urlWithForm(Configs.getStr(ConfigKey.URL_CONF_WHATS_LINK), urlParam, StandardCharsets.UTF_8, false);
        HttpRequest httpRequest = HttpUtil.createGet(s).timeout(6000);
        try (HttpResponse response = httpRequest.execute()){
            return JSONObject.parseObject(response.body(), AnalysisMagnetLinkResp.class);
        }catch (Exception e){
            log.error("预览磁力异常 {}",link,e);
            return null;
        }
    }

}
