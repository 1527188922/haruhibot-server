package com.haruhi.botServer.service.pixiv;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.Pixiv;
import com.haruhi.botServer.mapper.PixivMapper;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

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
    public void roundSend(WebSocketSession session,int num, Boolean isR18, List<String> tags, Message message, String tag) {
        List<Pixiv> pixivs = null;
        HashSet<Pixiv> pixivHashSet = null;
        boolean noTag = CollectionUtils.isEmpty(tags);
        if (noTag) {
            pixivs = pixivMapper.roundByTagLimit(num, isR18, null);
        } else {
            pixivs = pixivMapper.roundByTagsAll(isR18,tags);

        }
        if (CollectionUtils.isEmpty(pixivs)) {
            empty(session,noTag,tag,message);
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
            Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),message.getSelf_id(),BotConfig.NAME,forwardMessage);
        }
    }
    private void empty(WebSocketSession session,boolean noTag, String tag, Message message){
        if(noTag){
            Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(),"pix?????????????????????~",true);
        }else{
            Server.sendMessage(session,message.getUser_id(),message.getGroup_id(),message.getMessage_type(), MessageFormat.format("??????[{0}]?????????????????????tag?????????~", tag),true);
        }
    }
    private List<String> createForwardMessage(Collection<Pixiv> pixivs){
        List<String> strings = new ArrayList<>(pixivs.size());

        for (Pixiv pixiv : pixivs) {
            strings.add(MessageFormat.format("?????????{0}\n?????????{1}\nuid???{2}\npid???{3}\nr18???{4}\n?????????{5}", pixiv.getTitle(), pixiv.getAuthor(),pixiv.getUid(), pixiv.getPid(), pixiv.getIsR18() ? "???" : "???", pixiv.getImgUrl()));
        }
        return strings;
    }

}

