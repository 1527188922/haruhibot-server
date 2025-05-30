package com.haruhi.botServer.handlers.message.wordStrip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.WordStrip;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.wordStrip.WordStripService;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
public class WordStripDeleteHandler implements IGroupMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_660.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_660.getName();
    }

    @Autowired
    private WordStripService wordStripService;

    @Override
    public boolean onGroup(Bot bot, Message message) {
        String keyWord = null;
        if(message.getRawMessage().startsWith(RegexEnum.WORD_STRIP_DELETE.getValue())){
            keyWord = message.getRawMessage().replaceFirst(RegexEnum.WORD_STRIP_DELETE.getValue(),"");
        }

        if(Strings.isBlank(keyWord)){
            return false;
        }

        final String finalKeyWord = keyWord;
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            LambdaQueryWrapper<WordStrip> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WordStrip::getGroupId,message.getGroupId()).eq(WordStrip::getSelfId,message.getSelfId()).eq(WordStrip::getKeyWord, finalKeyWord);
            WordStrip one = wordStripService.getOne(queryWrapper);
            if(one == null){
                bot.sendGroupMessage(message.getGroupId(),MessageFormat.format("词条不存在：{0}",finalKeyWord),false);
                return;
            }
            if(!one.getUserId().equals(message.getUserId())){
                bot.sendGroupMessage(message.getGroupId(),
                        MessageFormat.format("你不是该词条的创建人，不可删除：{0}\n创建人:{1}",finalKeyWord,String.valueOf(one.getUserId())),false);
                return;
            }
            try {
                if (wordStripService.removeById(one.getId())) {
                    WordStripHandler.removeCache(message.getSelfId(),message.getGroupId(),finalKeyWord);
                    bot.sendGroupMessage(message.getGroupId(),MessageFormat.format("删除词条成功：{0}",finalKeyWord),false);
                }
            }catch (Exception e){
                bot.sendGroupMessage(message.getGroupId(),MessageFormat.format("删除词条异常：{0}",e.getMessage()),true);
                log.error("删除词条异常",e);
            }

        });
        return true;
    }
}
