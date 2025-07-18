package com.haruhi.botServer.handlers.message.wordStrip;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.entity.WordStripSqlite;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.service.WordStripSqliteService;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WordStripAddHandler implements IGroupMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_700.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_700.getName();
    }

    @Autowired
    private WordStripSqliteService wordStripService;

    @Override
    public boolean onGroup(Bot bot, final Message message) {

        String keyWord = null;
        String answer = null;
        Pattern compile = Pattern.compile(RegexEnum.WORD_STRIP_ADD.getValue());
        Matcher matcher = compile.matcher(message.getRawMessage());
        if(matcher.find()){
            keyWord = matcher.group(1);
            if(Strings.isNotBlank(keyWord)){
                answer = message.getRawMessage().substring(message.getRawMessage().indexOf("答") + 1);
            }
        }

        if(Strings.isBlank(keyWord) || Strings.isBlank(answer)){
            return false;
        }

        final String finalKeyWord = keyWord;
        final String finalAnswer = answer;
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {

                LambdaQueryWrapper<WordStripSqlite> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(WordStripSqlite::getGroupId,message.getGroupId())
                        .eq(WordStripSqlite::getSelfId,message.getSelfId())
                        .eq(WordStripSqlite::getKeyWord, finalKeyWord);
                WordStripSqlite wordStrip = wordStripService.getOne(queryWrapper);
                WordStripSqlite param = new WordStripSqlite();
                boolean save = false;
                if(wordStrip != null){
                    // 词条已存在
                    if(wordStrip.getUserId().equals(message.getUserId())){
                        // 修改人为创建人
                        param.setAnswer(finalAnswer);
                        param.setModifyTime(DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
                        save = wordStripService.update(param,queryWrapper);
                    }else{
                        bot.sendGroupMessage(message.getGroupId(),MessageFormat.format("已存在词条：{0}",finalKeyWord),false);
                        return;
                    }
                }else{
                    param.setKeyWord(finalKeyWord);
                    param.setAnswer(finalAnswer);
                    param.setGroupId(message.getGroupId());
                    param.setUserId(message.getUserId());
                    param.setSelfId(message.getSelfId());
                    Date date = new Date();
                    param.setCreateTime(DateTimeUtil.dateTimeFormat(date, DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
                    param.setModifyTime(DateTimeUtil.dateTimeFormat(date, DateTimeUtil.PatternEnum.yyyyMMddHHmmss));
                    save = wordStripService.save(param);
                }
                if(save){
                    WordStripHandler.putCache(message.getSelfId(),message.getGroupId(),finalKeyWord,finalAnswer);
                    bot.sendGroupMessage(message.getGroupId(),MessageFormat.format("词条添加成功：{0}",finalKeyWord),false);
                    return;
                }
                bot.sendGroupMessage(message.getGroupId(), MessageFormat.format("词条添加失败：{0}-->0",finalKeyWord),false);
            }catch (Exception e){
                log.error("添加词条异常",e);
            }

        });
        return true;
    }

}
