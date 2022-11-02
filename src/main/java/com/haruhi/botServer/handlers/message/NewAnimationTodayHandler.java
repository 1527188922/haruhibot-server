package com.haruhi.botServer.handlers.message;

import com.alibaba.fastjson.JSONArray;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.agefans.response.NewAnimationTodayResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.utils.HttpClientUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NewAnimationTodayHandler implements IMessageEvent {

    @Override
    public int weight() {
        return 85;
    }

    @Override
    public String funName() {
        return "今日新番";
    }

    @Override
    public boolean onMessage(final WebSocketSession session, final Message message, final String command) {
        if(!command.matches(RegexEnum.NEW_ANIMATION_TODAY.getValue())){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(new Task(session,message));
        return true;
    }

    private class Task implements Runnable{
        private Message message;
        private WebSocketSession session;

        Task(WebSocketSession session,Message message){
            this.session = session;
            this.message = message;
        }
        @Override
        public void run() {
            try {
                String responseHtml = HttpClientUtil.doGet(HttpClientUtil.getHttpClient(10 * 1000),ThirdPartyURL.AGEFANSTV, null);
                if (Strings.isNotBlank(responseHtml)) {
                    Pattern compile = Pattern.compile("var new_anime_list = (.*?);");
                    Matcher matcher = compile.matcher(responseHtml);
                    if (matcher.find()) {
                        String group = matcher.group(1);
                        List<NewAnimationTodayResp> data = JSONArray.parseArray(group, NewAnimationTodayResp.class);
                        if (CollectionUtils.isEmpty(data)) {
                            return;
                        }
                        data = data.stream().filter(e -> e.getIsnew()).collect(Collectors.toList());
                        if(data.size() > 0){
                            List<String> param = new ArrayList<>(data.size());
                            for (NewAnimationTodayResp datum : data) {
                                param.add(splicingParam(datum));
                            }
                            Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),message.getSelf_id(),BotConfig.NAME,param);
                        }else{
                            Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(), "今日还没有新番更新",true);
                        }
                    }
                }
            }catch (Exception e){
                Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(), MessageFormat.format("今日新番异常",e.getMessage()),true);
            }

        }
    }

    private String splicingParam(NewAnimationTodayResp datum){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(datum.getName()).append("\n");
        stringBuilder.append("更新集：").append(datum.getNamefornew()).append("\n");
        stringBuilder.append(MessageFormat.format("链接：{0}/detail/{1}", ThirdPartyURL.AGEFANSTV,datum.getId()));
        return stringBuilder.toString();
    }


}
