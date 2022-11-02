package com.haruhi.botServer.handlers.message.wordStrip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.WordStrip;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.wordStrip.WordStripService;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.text.MessageFormat;

@Slf4j
@Component
public class WordStripDeleteHandler implements IGroupMessageEvent {
    @Override
    public int weight() {
        return 94;
    }

    @Override
    public String funName() {
        return "删除词条";
    }

    @Autowired
    private WordStripService wordStripService;

    @Override
    public boolean onGroup(final WebSocketSession sessions,final Message message, final String command) {
        String keyWord = null;
        if(command.startsWith(RegexEnum.WORD_STRIP_DELETE.getValue())){
            keyWord = command.replaceFirst(RegexEnum.WORD_STRIP_DELETE.getValue(),"");
        }

        if(Strings.isBlank(keyWord)){
            return false;
        }

        final String finalKeyWord = keyWord;
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            LambdaQueryWrapper<WordStrip> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WordStrip::getGroupId,message.getGroup_id()).eq(WordStrip::getSelfId,message.getSelf_id()).eq(WordStrip::getKeyWord, finalKeyWord);
            WordStrip one = wordStripService.getOne(queryWrapper);
            if(one == null){
                Server.sendGroupMessage(sessions,message.getGroup_id(),MessageFormat.format("词条不存在：{0}",finalKeyWord),false);
                return;
            }
            if(!one.getUserId().equals(message.getUser_id())){
                Server.sendGroupMessage(sessions,message.getGroup_id(),
                        MessageFormat.format("你不是该词条的创建人，不可删除：{0}\n创建人:{1}",finalKeyWord,one.getUserId()),false);
                return;
            }
            try {
                if (wordStripService.removeById(one.getId())) {
                    WordStripHandler.removeCache(message.getSelf_id(),message.getGroup_id(),finalKeyWord);
                    Server.sendGroupMessage(sessions,message.getGroup_id(),MessageFormat.format("删除词条成功：{0}",finalKeyWord),false);
                }
            }catch (Exception e){
                Server.sendGroupMessage(sessions,message.getGroup_id(),MessageFormat.format("删除词条异常：{0}",e.getMessage()),true);
                log.error("删除词条异常",e);
            }

        });
        return true;
    }
}
