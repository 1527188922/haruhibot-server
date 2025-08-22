package com.haruhi.botServer.handlers.message;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.DictionaryEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.qingyunke.ChatResp;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任何命令都没有匹配到
 * 并且群里at了机器人或者给机器人发私聊
 */
@Slf4j
@Component
public class AiChatHandler implements IAllMessageEvent {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_160.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_160.getName();
    }
    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    public MatchResult<String> matching(Message message) {
        if(message.isPrivateMsg()){
            if(!message.isTextMsgOnly()){
                return MatchResult.unmatched();
            }
            return MatchResult.matched(message.getText(-1));
        }
        if(message.isGroupMsg()){
            if(! (message.isAtBot() && message.isTextMsg() && !message.isPicMsg()) ){
                return MatchResult.unmatched();
            }
            String text = message.getText(-1);
            return StringUtils.isNotBlank(text) ? MatchResult.matched(text) : MatchResult.unmatched();
        }
        return MatchResult.unmatched();
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {

        boolean qingyunkeChat = dictionarySqliteService.getBoolean(DictionaryEnum.SWITCH_QINGYUNKE_CHAT.getKey(), false);
        if(!qingyunkeChat){
            return false;
        }
        MatchResult<String> matchResult = matching(message);
        if(!matchResult.isMatched()){
            return false;
        }

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            Map<String, Object> urlParam = new HashMap<>(3);
            urlParam.put("key","free");
            urlParam.put("appid",0);
            urlParam.put("msg",matchResult.getData());

            String s = HttpUtil.urlWithForm(ThirdPartyURL.QINGYUNKE_AI_CHAT, urlParam, StandardCharsets.UTF_8, false);
            HttpRequest httpRequest = HttpUtil.createGet(s).timeout(8000);
            try (HttpResponse response = httpRequest.execute()){
                String body = response.body();
                if (StringUtils.isBlank(body)) {
                    return;
                }
                ChatResp chatResp = JSONObject.parseObject(body, ChatResp.class);
                if(chatResp != null){
                    String content = chatResp.getContent();
                    if(content != null){
                        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),processContent(content, bot.getBotName()),false);
                    }
                }
            }catch (Exception e){
                log.error("青云客api请求异常",e);
            }
        });
        return true;
    }

    private static String reg ="(?<=\\{face:)[0-9]*(?=\\})";
    private static String regex = ".*\\{face:.*\\}.*";
    private String processContent(String content, String botname){
        content = content.replace("{br}", "\n").replace("菲菲", botname).replace("&quot;","“");
        if(!content.matches(regex)){
            return content;
        }
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(content);
        List<String> matchStrs = new ArrayList<>();

        while (matcher.find()) {
            matchStrs.add(matcher.group());
        }
        KQCodeUtils instance = KQCodeUtils.getInstance();
        for (int i = 0; i < matchStrs.size(); i++) {
            String id = matchStrs.get(i);
            String face = instance.toCq(CqCodeTypeEnum.face.getType(), "id="+id);
            content = content.replace("{face:" + id + "}", face);
        }
        return content;
    }
}
