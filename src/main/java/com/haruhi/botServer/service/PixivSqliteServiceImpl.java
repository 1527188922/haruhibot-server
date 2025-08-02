package com.haruhi.botServer.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.dto.qqclient.ForwardMsgItem;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.entity.PixivSqlite;
import com.haruhi.botServer.mapper.PixivSqliteMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PixivSqliteServiceImpl extends ServiceImpl<PixivSqliteMapper, PixivSqlite> implements PixivSqliteService {

    @Autowired
    private PixivSqliteMapper pixivSqliteMapper;

    @Override
    public void roundSend(Bot bot, int num, Boolean isR18, List<String> tags, Message message, String tag) {
        List<PixivSqlite> pixivs = null;
        HashSet<PixivSqlite> pixivHashSet = null;
        boolean noTag = CollectionUtils.isEmpty(tags);
        if (noTag) {
            pixivs = pixivSqliteMapper.roundByTagLimit(num, isR18, null);
        } else {
            pixivs = pixivSqliteMapper.roundByTagsAll(isR18,tags);

        }
        if (CollectionUtils.isEmpty(pixivs)) {
            empty(bot,noTag,tag,message);
            return;
        }
        int size = pixivs.size();
        if(size > num){
            pixivHashSet = new HashSet<>(num);
            while (pixivHashSet.size() < num){
                pixivHashSet.add(pixivs.get(CommonUtil.randomInt(0,size - 1)));
            }
        }else if(size == num || size > 0){
            pixivHashSet = new HashSet<>(pixivs);
        }
        List<String> messages = null;
        if(!pixivHashSet.isEmpty()){
            messages = pixFormat(pixivHashSet);
        }else{
            messages = pixFormat(pixivs);
        }
        if (!CollectionUtils.isEmpty(messages)) {

            List<ForwardMsgItem> forwardMsgItems = messages.stream()
                    .map(e -> ForwardMsgItem.instance(message.getSelfId(), bot.getBotName(), MessageHolder.instanceText(e)))
                    .collect(Collectors.toList());

            bot.sendForwardMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),forwardMsgItems);
        }
    }
    private void empty(Bot bot,boolean noTag, String tag, Message message){
        if(noTag){
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"pix图库还没有图片~",true);
        }else{
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageFormat.format("没有[{0}]的图片，换一个tag试试吧~", tag),true);
        }
    }
    private List<String> pixFormat(Collection<PixivSqlite> pixivs){
        List<String> strings = new ArrayList<>(pixivs.size());

        for (PixivSqlite pixiv : pixivs) {
            strings.add(MessageFormat.format(
                    "标题：{0}\n作者：{1}\nuid：{2}\npid：{3}\nr18：{4}\n1原图：{5}\n2原图：{6}",
                    pixiv.getTitle(),
                    pixiv.getAuthor(),
                    pixiv.getUid(),
                    pixiv.getPid(),
                    pixiv.getIsR18() == 1 ? "是" : "否", pixiv.getImgUrl(),
                    MessageFormat.format("https://pixiv.re/{0}-1.jpg",pixiv.getPid())
            ));
        }
        return strings;
    }

}
