package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.SwitchConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.aiChat.response.ChatResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.RestUtil;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.text.MessageFormat;
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
        return 1;
    }

    @Override
    public String funName() {
        return "智障聊天";
    }

    public MatchResult<String> matching(Message message) {
        if(message.isPrivateMsg()){
            // 私聊了机器人
            if(!message.isTextMsg()){
                return MatchResult.unmatched();
            }
            return MatchResult.matched(message.getText(-1));
        }
        if(message.isGroupMsg()){
            if(!message.isAtBot()){
                // 没有at机器人
                return MatchResult.unmatched();
            }
            String text = message.getText(-1);
            return StringUtils.isNotBlank(text) ? MatchResult.matched(text) : MatchResult.unmatched();
        }
        return MatchResult.unmatched();
    }

    @Override
    public boolean onMessage(final WebSocketSession session,final Message message) {
        if(!SwitchConfig.ENABLE_AI_CHAT){
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
            ChatResp chatResp = null;
            try {
                chatResp = RestUtil.sendGetRequest(RestUtil.getRestTemplate(8 * 1000), ThirdPartyURL.AI_CHAT, urlParam, ChatResp.class);
            }catch (Exception e){
                Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(), MessageFormat.format("聊天api请求异常:{0}",e.getMessage()),true);
                log.error("青云客api请求异常",e);
            }
            if(chatResp != null){
                String content = chatResp.getContent();
                if(content != null){
                    Server.sendMessage(session,message.getUserId(),message.getGroupId(),message.getMessageType(),processContent(content),false);
                }
            }
        });
        return true;
    }

    private static String reg ="(?<=\\{face:)[0-9]*(?=\\})";
    private static String regex = ".*\\{face:.*\\}.*";
    private String processContent(String content){
        content = content.replace("{br}", "\n").replace("菲菲", BotConfig.NAME).replace("&quot;","“");
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
