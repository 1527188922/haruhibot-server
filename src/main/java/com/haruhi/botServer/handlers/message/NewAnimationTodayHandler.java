//package com.haruhi.botServer.handlers.message;
//
//import com.alibaba.fastjson.JSONArray;
//import com.haruhi.botServer.constant.HandlerWeightEnum;
//import com.haruhi.botServer.constant.RegexEnum;
//import com.haruhi.botServer.dto.agefans.NewAnimationTodayResp;
//import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
//import com.haruhi.botServer.dto.qqclient.Message;
//import com.haruhi.botServer.dto.qqclient.MessageHolder;
//import com.haruhi.botServer.event.message.IAllMessageEvent;
//import com.haruhi.botServer.service.DictionarySqliteService;
//import com.haruhi.botServer.utils.ThreadPoolUtil;
//import com.haruhi.botServer.utils.HttpClientUtil;
//import com.haruhi.botServer.ws.Bot;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.util.CollectionUtils;
//
//import java.text.MessageFormat;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//public class NewAnimationTodayHandler implements IAllMessageEvent {
//
//    @Override
//    public int weight() {
//        return HandlerWeightEnum.W_450.getWeight();
//    }
//
//    @Override
//    public String funName() {
//        return HandlerWeightEnum.W_450.getName();
//    }
//
//    @Autowired
//    private DictionarySqliteService dictionarySqliteService;
//
//    @Override
//    public boolean onMessage(Bot bot, Message message) {
//        if(!(message.isTextMsg() && message.getText(-1).matches(RegexEnum.NEW_ANIMATION_TODAY.getValue()))){
//            return false;
//        }
//        String urlAgefans = dictionarySqliteService.getInCache(DictionarySqliteService.DictionaryEnum.URL_CONF_AGEFANS.getKey(), null);
//        ThreadPoolUtil.getHandleCommandPool().execute(()->{
//            try {
//
//                String responseHtml = null;
//                try {
//                    responseHtml = HttpClientUtil.doGetNoCatch(urlAgefans, null,10 * 1000);
//                }catch (Exception e){
//                    log.error("获取新番请求异常 {}",urlAgefans,e);
//                    bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), "获取新番异常\n"+e.getMessage(),true);
//                    return;
//                }
//                log.debug("age html : {}",responseHtml);
//                Pattern compile = Pattern.compile("var new_anime_list = (.*?);");
//                Matcher matcher = compile.matcher(responseHtml);
//                if (matcher.find()) {
//                    String group = matcher.group(1);
//                    List<NewAnimationTodayResp> data = JSONArray.parseArray(group, NewAnimationTodayResp.class);
//                    if (CollectionUtils.isEmpty(data)) {
//                        return;
//                    }
//                    data = data.stream().filter(NewAnimationTodayResp::getIsnew).collect(Collectors.toList());
//                    if(CollectionUtils.isEmpty(data)){
//                        bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), "今日还没有番剧更新",true);
//                        return;
//                    }
//                    List<ForwardMsgItem> forwardMsgItems = new ArrayList<>(data.size());
//                    for (NewAnimationTodayResp datum : data) {
//                        String splicingParam = splicingParam(datum, urlAgefans);
//                        ForwardMsgItem instance = ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(splicingParam));
//                        forwardMsgItems.add(instance);
//                    }
//                    bot.sendForwardMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), forwardMsgItems);
//                }
//            }catch (Exception e){
//                log.error("解析新番数据异常",e);
//                bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageFormat.format("今日新番异常:{0}",e.getMessage()),true);
//            }
//        });
//        return true;
//    }
//
//    private String splicingParam(NewAnimationTodayResp datum,String urlAgefans){
//        StringBuilder stringBuilder = new StringBuilder();
//        stringBuilder.append(datum.getName()).append("\n");
//        stringBuilder.append("更新集：").append(datum.getNamefornew()).append("\n");
//        stringBuilder.append(MessageFormat.format("链接：{0}/detail/{1}", urlAgefans,datum.getId()));
//        return stringBuilder.toString();
//    }
//
//
//}
