package com.haruhi.botserver.features.reply.handler;

import com.haruhi.botserver.bot.handler.IGroupMessageHandler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.ForwardMsgItem;
import com.haruhi.botserver.integration.onebot.model.GroupMember;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import com.haruhi.botserver.integration.onebot.model.SyncResponse;
import com.haruhi.botserver.shared.util.CommonUtil;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class FakeMessageHandler implements IGroupMessageHandler {
    

    @Override
    public int weight() {
        return HandlerWeightEnum.W_430.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_430.getName();
    }

    @Override
    public boolean onGroup(Bot bot, Message message) {

        String word = CommonUtil.commandReplaceFirst(message.getRawMessage(), RegexEnum.FRIEND_SAID);
        if(Strings.isBlank(word)){
            return false;
        }
        word = word.replaceFirst("他|她|它","我");
        String finalWord = word;
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                SyncResponse<List<GroupMember>> syncResponse = bot.getGroupMemberList(message.getGroupId(), Duration.ofSeconds(10).toMillis());

                List<GroupMember> groupMemberList = syncResponse.getData();
                if(!CollectionUtils.isEmpty(groupMemberList)){
                    List<Long> longs = Arrays.asList(message.getSelfId(), message.getUserId());
                    groupMemberList.removeIf(next -> longs.contains(next.getUserId()));
                }
                if(CollectionUtils.isEmpty(groupMemberList)){
//                    Server.sendGroupMessage(session,message.getGroupId(),"你哪来的朋友？",true);
                    return;
                }
                int i = CommonUtil.randomInt(0, groupMemberList.size() - 1);
                GroupMember friend = groupMemberList.get(i);

                ForwardMsgItem instance = ForwardMsgItem.instance(friend.getUserId(), friend.getNickname(), MessageHolder.instanceText(finalWord));
                bot.sendForwardMessage(message.getUserId(), message.getGroupId(), message.getMessageType(), Collections.singletonList(instance));
            }catch (Exception e){
                log.error("发送假消息异常",e);
            }
        });
        return true;
    }
}
