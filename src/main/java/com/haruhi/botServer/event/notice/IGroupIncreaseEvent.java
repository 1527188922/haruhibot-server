package com.haruhi.botServer.event.notice;


import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.ws.Bot;

/**
 * 群加入成员事件
 */
public interface IGroupIncreaseEvent extends INoticeEvent {

    /**
     * user_id : 进群人的qq
     * self_id : 机器人qq
     * group_id : 群号
     * time : 时间 (秒级时间戳)
     * @param message
     */
    void onGroupIncrease(Bot bot, Message message);
}
