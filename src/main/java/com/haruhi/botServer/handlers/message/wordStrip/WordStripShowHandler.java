package com.haruhi.botServer.handlers.message.wordStrip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.WordStripSqlite;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.service.WordStripSqliteService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.List;

@Slf4j
@Component
public class WordStripShowHandler implements IGroupMessageEvent {
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
