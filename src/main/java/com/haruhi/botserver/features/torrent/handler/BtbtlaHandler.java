package com.haruhi.botserver.features.torrent.handler;

import com.haruhi.botserver.bot.handler.IAllMessageHandler;

import cn.hutool.core.text.StrFormatter;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.features.torrent.client.model.btbtla.SearchResult;
import com.haruhi.botserver.integration.onebot.model.ForwardMsgItem;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.features.torrent.service.BtbtlaService;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Component
public class BtbtlaHandler implements IAllMessageHandler {


    @Autowired
    private BtbtlaService btbtlaService;

    @Override
    public int weight() {
        return HandlerWeightEnum.W_460.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_460.getName();
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if(!message.isTextMsgOnly()){
            return false;
        }

        String trim = message.getText(-1).trim();
        if (!trim.startsWith("bt")) {
            return false;
        }
        String keyword = trim.replaceFirst("bt", "");
        if(StringUtils.isBlank(keyword)){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            SearchResult searchResult = btbtlaService.search(keyword);
            if (searchResult.getException() != null) {
                log.error("bt影视搜索异常 {}",keyword,searchResult.getException());
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText(
                        StrFormatter.format("搜索【{}】异常\n{}",keyword,searchResult.getException().getMessage())
                ));
                return;
            }
            List<SearchResult.ModuleItem> moduleItems = searchResult.getModuleItems();
            if (CollectionUtils.isEmpty(moduleItems)) {
                bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), MessageHolder.instanceText(
                        StrFormatter.format("无搜索结果【{}】",keyword)
                ));
                return;
            }
            List<ForwardMsgItem> forwardMsgItems = new ArrayList<>();
            forwardMsgItems.add(ForwardMsgItem.instance(bot.getId(),bot.getBotName(),
                    MessageHolder.instanceText(StrFormatter.format("搜索【{}】完成", keyword))));
            forwardMsgItems.addAll(resultToForwardMsgItemList(bot, moduleItems));
            bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), forwardMsgItems);
        });
        return true;
    }

    private List<ForwardMsgItem> resultToForwardMsgItemList(Bot bot, List<SearchResult.ModuleItem> moduleItems) {
        return moduleItems.stream().map(item->{
            List<MessageHolder> messageHolders = MessageHolder.instanceText(StrFormatter.format(
                    "{}\n{}\n{}\n{}",
                    item.getTitle(),
                    Stream.of(item.getYear(),item.getCountry(),item.getCategory()).filter(StringUtils::isNotBlank).collect(Collectors.joining(" | ")),
                    "简介："+item.getDescription(),
                    "磁力下载："+btbtlaService.getDomain()+item.getDetailHref()

            ));
            messageHolders.add(0, MessageHolder.instanceImage(item.getItemPicUrl()));
            return ForwardMsgItem.instance(bot.getId(), bot.getBotName(), messageHolders);
        }).collect(Collectors.toList());
    }

}
