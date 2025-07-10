package com.haruhi.botServer.handlers.message.pixiv;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.PixivSqlite;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.service.PixivSqliteService;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
public class PixivCountHandler implements IAllMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_780.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_780.getName();
    }

    @Autowired
    private PixivSqliteService pixivSqliteService;

    @Override
    public boolean onMessage(final Bot bot, final Message message) {
        if(!message.getRawMessage().matches(RegexEnum.PIXIV_COUNT.getValue())){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            LambdaQueryWrapper<PixivSqlite> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(PixivSqlite::getIsR18,0);
            LambdaQueryWrapper<PixivSqlite> queryWrapperR18 = new LambdaQueryWrapper<>();
            queryWrapperR18.eq(PixivSqlite::getIsR18,1);
            int count = pixivSqliteService.count(queryWrapper);
            int countR18 = pixivSqliteService.count(queryWrapperR18);
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                    MessageFormat.format("pixiv库：\n非r18：{0}\nr18：{1}\n总计：{2}",count,countR18,count + countR18)
                    ,true);
        });

        return true;
    }
}
