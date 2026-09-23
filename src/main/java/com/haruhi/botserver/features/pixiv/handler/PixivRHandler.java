package com.haruhi.botserver.features.pixiv.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.features.pixiv.service.PixivSqliteService;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class PixivRHandler implements IAllMessageHandler {

    @Autowired
    private PixivSqliteService pixivSqliteService;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_820.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_820.getName();
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        String[] split = RegexEnum.PIXIV_R.getValue().split("\\|");
        boolean flag = false;
        String tag = null;
        List<String> tags = null;
        for (String s : split) {
            if (message.getRawMessage().startsWith(s)) {
                flag = true;
                tag = message.getRawMessage().replaceFirst(s,"");
                if(Strings.isBlank(tag)){
                    tags = new ArrayList<>(1);
                }else{
                    tags = Arrays.asList(tag.split(",|，"));
                }
                break;
            }
        }
        if(!flag){
            return false;
        }
        List<String> finalTags = tags;
        String finalTag = tag;
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            pixivSqliteService.roundSend(bot,20,true, finalTags,message, finalTag);
        });

        return true;
    }
}
