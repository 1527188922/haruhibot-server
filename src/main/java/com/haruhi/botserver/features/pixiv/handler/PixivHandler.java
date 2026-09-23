package com.haruhi.botserver.features.pixiv.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.features.pixiv.service.PixivSqliteService;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import com.simplerobot.modules.utils.KQCodeUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Component
public class PixivHandler implements IAllMessageHandler {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_740.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_740.getName();
    }

    @Autowired
    private PixivSqliteService pixivSqliteService;


    @Override
    public boolean onMessage(final Bot bot, final Message message) {
        List<String> tags = null;
        String tag = null;
        String cq = KQCodeUtils.getInstance().getCq(message.getRawMessage(), 0);
        if(cq != null){
            return false;
        }

        boolean flag = false;

        String[] split = RegexEnum.PIXIV.getValue().split("\\|");
        for (String s : split) {
            if (message.getRawMessage().startsWith(s)) {
                tag = message.getRawMessage().replace(s,"");
                if(Strings.isBlank(tag.trim())){
                    tags = new ArrayList<>(1);
                }else{
                    tags = Arrays.asList(tag.split(",|，"));
                }
                flag = true;
            }
        }
        if (!flag) {
            return false;
        }


        List<String> finalTags = tags;
        String finalTag = tag;
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            pixivSqliteService.roundSend(bot,20,null, finalTags,message, finalTag);
        });
        return true;
    }

}
