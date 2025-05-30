package com.haruhi.botServer.handlers.message.pixiv;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.entity.Pixiv;
import com.haruhi.botServer.event.message.IAllMessageEvent;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.service.pixiv.PixivService;
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
    private PixivService pixivService;

    @Override
    public boolean onMessage(final Bot bot, final Message message) {
        if(!message.getRawMessage().matches(RegexEnum.PIXIV_COUNT.getValue())){
            return false;
        }
        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            QueryWrapper<Pixiv> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(Pixiv::getIsR18,false);
            QueryWrapper<Pixiv> queryWrapperR18 = new QueryWrapper<>();
            queryWrapperR18.lambda().eq(Pixiv::getIsR18,true);
            int count = pixivService.count(queryWrapper);
            int countR18 = pixivService.count(queryWrapperR18);
            bot.sendMessage(message.getUserId(),message.getGroupId(),message.getMessageType(),
                    MessageFormat.format("pixiv库：\n非r18：{0}\nr18：{1}\n总计：{2}",count,countR18,count + countR18)
                    ,true);
        });

        return true;
    }
}
