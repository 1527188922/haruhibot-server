package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.AnalysisMagnetLinkResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IPrivateMessageEvent;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.RestUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LinkPreviewHandler implements IPrivateMessageEvent {


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
                bot.sendPrivateMessage(message.getUserId(),
                        "磁力未解析出结果\n"+link,
                        true);
                return;
            }
            if(StringUtils.isNotBlank(resp.getError())){
                bot.sendPrivateMessage(message.getUserId(),
                        "磁力解析异常\n"+resp.getError(),
                        true);
                return;
            }
            bot.sendPrivateMessage(message.getUserId(), formatterResp(resp),true);
        });
        return true;
    }

    private String formatterResp(AnalysisMagnetLinkResp resp){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(resp.getName()).append("\n")
                .append("类型：").append(resp.getFileType()).append("\n")
                .append("文件数量：").append(resp.getCount());
        if(resp.getSize() != null && resp.getSize() != 0){
            double v = (double) resp.getSize() / 1024D / 1024D; //MB
            String size = "";
            if(v >= 1024D){
                size = String.format("%.3f",(v / 1024D)); //GB
            }else{
                size = String.format("%.3f",v);
            }
            stringBuilder.append("\n").append("总大小：").append(size).append("MB");
        }
        if (!CollectionUtils.isEmpty(resp.getScreenshots())) {
            List<String> collect = resp.getScreenshots().stream().map(AnalysisMagnetLinkResp.Screenshots::getScreenshot).collect(Collectors.toList());
            String join = StringUtils.join(collect, "\n");
            stringBuilder.append("\n").append(join);
        }
        return stringBuilder.toString();
    }

    private AnalysisMagnetLinkResp request(String link){
        RestTemplate restTemplate = RestUtil.getRestTemplate(6 * 1000);
        HashMap<String, Object> urlParam = new HashMap<>();
        urlParam.put("url",link);
        return RestUtil.sendGetRequest(restTemplate, ThirdPartyURL.WHATS_LINK, urlParam, AnalysisMagnetLinkResp.class);
    }

}
