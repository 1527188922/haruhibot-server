package com.haruhi.botServer.service.pixiv;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.Pixiv;
import com.haruhi.botServer.mapper.PixivMapper;
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


@Service
public class PixivServiceImpl extends ServiceImpl<PixivMapper, Pixiv> implements PixivService{

    @Autowired
    private PixivMapper pixivMapper;

    @Override
    public void roundSend(Bot bot, int num, Boolean isR18, List<String> tags, Message message, String tag) {
        List<Pixiv> pixivs = null;
        HashSet<Pixiv> pixivHashSet = null;
        boolean noTag = CollectionUtils.isEmpty(tags);
        if (noTag) {
            pixivs = pixivMapper.roundByTagLimit(num, isR18, null);
        } else {
            pixivs = pixivMapper.roundByTagsAll(isR18,tags);

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
        List<String> forwardMessage = null;
        if(pixivHashSet.size() > 0){
            forwardMessage = createForwardMessage(pixivHashSet);
        }else{
            forwardMessage = createForwardMessage(pixivs);
        }
        if (!CollectionUtils.isEmpty(forwardMessage)) {
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),message.getSelfId(),BotConfig.NAME,forwardMessage);
        }
    }
    private void empty(Bot bot,boolean noTag, String tag, Message message){
        if(noTag){
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),"pix图库还没有图片~",true);
        }else{
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(), MessageFormat.format("没有[{0}]的图片，换一个tag试试吧~", tag),true);
        }
    }
    private List<String> createForwardMessage(Collection<Pixiv> pixivs){
        List<String> strings = new ArrayList<>(pixivs.size());

        for (Pixiv pixiv : pixivs) {
            strings.add(MessageFormat.format(
                    "标题：{0}\n作者：{1}\nuid：{2}\npid：{3}\nr18：{4}\n1原图：{5}\n2原图：{6}",
                    pixiv.getTitle(),
                    pixiv.getAuthor(),
                    pixiv.getUid(), 
                    pixiv.getPid(), 
                    pixiv.getIsR18() ? "是" : "否", pixiv.getImgUrl(),
                    MessageFormat.format("https://pixiv.re/{0}-1.jpg",pixiv.getPid())
            ));
        }
        return strings;
    }

}

