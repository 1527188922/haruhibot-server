package com.haruhi.botServer.handlers.message.wordStrip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.WordStrip;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.factory.ThreadPoolFactory;
import com.haruhi.botServer.service.wordStrip.WordStripService;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.text.MessageFormat;
import java.util.List;

@Slf4j
@Component
public class WordStripShowHandler implements IGroupMessageEvent {
    @Override
    public int weight() {
        return 93;
    }

    @Override
    public String funName() {
        return "本群词条";
    }

    @Autowired
    private WordStripService wordStripService;

    @Override
    public boolean onGroup(final WebSocketSession session,final Message message, final String command) {
        if (!command.matches(RegexEnum.WORD_STRIP_SHOW.getValue())) {
            return false;
        }
        ThreadPoolFactory.getCommandHandlerThreadPool().execute(()->{
            LambdaQueryWrapper<WordStrip> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WordStrip::getGroupId,message.getGroup_id()).eq(WordStrip::getSelfId,message.getSelf_id());
            List<WordStrip> list = wordStripService.list(queryWrapper);
            if(CollectionUtils.isEmpty(list)){
                Server.sendGroupMessage(session,message.getGroup_id(),"本群没有词条",true);
                return;
            }
            Server.sendGroupMessage(session,message.getGroup_id(), processWordStrip(list),false);
        });
        return true;
    }

    private String processWordStrip(List<WordStrip> list){
        StringBuilder stringBuilder = new StringBuilder("本群词条：\n");
        for (WordStrip wordStrip : list) {
            stringBuilder.append(MessageFormat.format("[{0}]-[{1}] 创建人：{2}\n",wordStrip.getKeyWord(),wordStrip.getAnswer(),wordStrip.getUserId()));
        }
        return stringBuilder.toString();
    }
}
