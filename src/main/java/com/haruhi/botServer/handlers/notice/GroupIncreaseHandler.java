package com.haruhi.botServer.handlers.notice;

import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.notice.IGroupIncreaseEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.text.MessageFormat;

@Slf4j
@Component
public class GroupIncreaseHandler implements IGroupIncreaseEvent {


    @Override
    public void onGroupIncrease(final WebSocketSession session,final Message message) {
        ThreadPoolUtil.getHandleCommandPool().execute(()->{

            KQCodeUtils instance = KQCodeUtils.getInstance();
            String at = instance.toCq(CqCodeTypeEnum.at.getType(), "qq=" + message.getUserId());
            String faces = "";
            String face = instance.toCq(CqCodeTypeEnum.face.getType(), "id=" + 144);
            for (int i = 0; i < 3; i++) {
                faces += face;
            }
            Server.sendGroupMessage(session,message.getGroupId(), MessageFormat.format("{0} 欢迎小可爱~{1}",at,faces),false);
        });
    }
}
