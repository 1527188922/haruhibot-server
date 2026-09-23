package com.haruhi.botserver.features.pixiv.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.shared.constant.RegexEnum;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.features.pixiv.persistence.entity.PixivSqlite;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.features.pixiv.service.PixivSqliteService;
import com.haruhi.botserver.infrastructure.concurrent.ThreadPoolUtil;
import com.haruhi.botserver.bot.session.Bot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;

@Slf4j
@Component
public class PixivCountHandler implements IAllMessageHandler {
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
            long count = pixivSqliteService.count(queryWrapper);
            long countR18 = pixivSqliteService.count(queryWrapperR18);
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                    MessageFormat.format("pixiv库：\n非r18：{0}\nr18：{1}\n总计：{2}",count,countR18,count + countR18)
                    ,true);
        });

        return true;
    }
}
