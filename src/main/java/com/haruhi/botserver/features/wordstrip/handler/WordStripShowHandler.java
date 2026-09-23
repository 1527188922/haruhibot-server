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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.List;

@Slf4j
@Component
public class WordStripShowHandler implements IGroupMessageHandler {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_640.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_640.getName();
    }

    @Autowired
    private WordStripSqliteService wordStripService;

    @Override
    public boolean onGroup(Bot bot, final Message message) {
        if (!message.getRawMessage().matches(RegexEnum.WORD_STRIP_SHOW.getValue())) {
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            LambdaQueryWrapper<WordStripSqlite> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WordStripSqlite::getGroupId,message.getGroupId()).eq(WordStripSqlite::getSelfId,message.getSelfId());
            List<WordStripSqlite> list = wordStripService.list(queryWrapper);
            if(CollectionUtils.isEmpty(list)){
                bot.sendGroupMessage(message.getGroupId(),"本群没有词条",true);
                return;
            }
            bot.sendGroupMessage(message.getGroupId(), processWordStrip(list),false);
        });
        return true;
    }

    private String processWordStrip(List<WordStripSqlite> list){
        StringBuilder stringBuilder = new StringBuilder("本群词条：\n");
        for (WordStripSqlite wordStrip : list) {
            stringBuilder.append(MessageFormat.format("[{0}]-[{1}] 创建人：{2}\n",wordStrip.getKeyWord(),wordStrip.getAnswer(),String.valueOf(wordStrip.getUserId())));
        }
        return stringBuilder.toString();
    }
}
