package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.session.Bot;

/**
 * 群加入成员事件
 */
public interface IGroupIncreaseHandler extends INoticeHandler {

    /**
     * user_id : 进群人的qq
     * self_id : 机器人qq
     * group_id : 群号
     * time : 时间 (秒级时间戳)
     * @param message
     */
    void onGroupIncrease(Bot bot, Message message);
}
