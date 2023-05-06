package com.haruhi.botServer.handlers.message;

import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.GroupMember;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.GocqSyncRequestUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class FriendSaidHandler implements IGroupMessageEvent {
    

    @Override
    public int weight() {
        return 84;
    }

    @Override
    public String funName() {
        return "朋友说";
    }

    @Override
    public boolean onGroup(final WebSocketSession session,final Message message,final String command) {

        String say = CommonUtil.commandReplaceFirst(command, RegexEnum.FRIEND_SAID);
        if(Strings.isBlank(say)){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(new FriendSaidHandler.SayTask(session, message, say));
        return true;
    }

    private static class SayTask implements Runnable{
        private final WebSocketSession session;
        private final Message message;
        private final String say;
        SayTask(WebSocketSession session, Message message, String say) {
            this.session = session;
            this.message = message;
            this.say = say;
        }

        @Override
        public void run() {
            try {
                List<GroupMember> groupMemberList = GocqSyncRequestUtil.getGroupMemberList(session,message.getGroupId(), 
                        Arrays.asList(message.getSelfId(),message.getUserId()), 2L * 1000L);
                if(CollectionUtils.isEmpty(groupMemberList)){
//                    Server.sendGroupMessage(session,message.getGroupId(),"你哪来的朋友？",true);
                    return;
                }
                int i = CommonUtil.randomInt(0, groupMemberList.size() - 1);
                GroupMember friend = groupMemberList.get(i);
                Server.sendGroupMessage(session,message.getGroupId(),friend.getUserId(),friend.getNickname(),Collections.singletonList(say));
                
            }catch (Exception e){
                log.error("朋友说发生异常",e);
            }
        }

    }
}
