package com.haruhi.botserver.features.news.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.infrastructure.logging.DbLog;
import com.haruhi.botserver.integration.onebot.model.ForwardMsgItem;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class NewsHandler implements IAllMessageHandler {

    private final List<NewsImageProvider> providers;

    public NewsHandler(List<NewsImageProvider> providers) {
        providers.sort(Comparator.comparing(NewsImageProvider::sort));
        this.providers = providers;
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_400.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_400.getName();
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if (!message.isTextMsgOnly()) {
            return false;
        }
        String text = message.getText(-1);
        if(StringUtils.isNotBlank(text) &&
                !text.matches(RegexEnum.SEE_TODAY_NEWS.getValue())){
            return false;
        }
        handle(bot, message);
        return true;
    }

    private void handle(Bot bot, Message message){

        List<ForwardMsgItem> forwardMsgItems = new ArrayList<>();
        for (NewsImageProvider provider : providers) {
            try {
                String imageUrl = provider.imageUrl();
                if (StringUtils.isBlank(imageUrl)) {
                    continue;
                }
                forwardMsgItems.add(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceImage(imageUrl)));
            } catch (Exception e) {
                DbLog.error(BusinessModuleEnum.NEWS, "获取新闻图片异常 provider:{}", provider.name(), e);
            }
        }

        if (forwardMsgItems.isEmpty()) {
            bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                    MessageHolder.instanceText("今日新闻图片获取失败"));
            return;
        }

        String date = DateTimeUtil.dateTimeFormat(System.currentTimeMillis(), DateTimeUtil.PatternEnum.yyyyMMdd);
        forwardMsgItems.addFirst(ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(),
                MessageHolder.instanceText(date + "新闻")));
        bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgItems);
    }
}
