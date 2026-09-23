package com.haruhi.botserver.features.wordstrip.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.features.wordstrip.persistence.entity.WordStripSqlite;
import com.haruhi.botserver.bot.handler.IGroupMessageHandler;
import com.haruhi.botserver.features.wordstrip.service.WordStripSqliteService;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
public class WordStripDeleteHandler implements IGroupMessageHandler {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_660.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_660.getName();
    }

    @Autowired
    private WordStripSqliteService wordStripService;

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
            LambdaQueryWrapper<WordStripSqlite> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WordStripSqlite::getGroupId,message.getGroupId())
                    .eq(WordStripSqlite::getSelfId,message.getSelfId())
                    .eq(WordStripSqlite::getKeyWord, finalKeyWord);
            WordStripSqlite one = wordStripService.getOne(queryWrapper);
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
