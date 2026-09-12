package com.haruhi.botServer.handler.message.bilibili;

import cn.hutool.core.text.StrFormatter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botServer.constant.BilibiliSubscribeTypeEnum;
import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.dto.qqclient.MessageHolder;
import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import com.haruhi.botServer.handler.message.IAllMessageHandler;
import com.haruhi.botServer.service.BilibiliSubscribeSqliteService;
import com.haruhi.botServer.ws.Bot;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * b站直播订阅/取消订阅指令处理基类
 * 群消息订阅本群，私聊消息订阅发消息的人
 */
public abstract class AbstractBilibiliSubscribeHandler implements IAllMessageHandler {

    @Autowired
    protected BilibiliSubscribeSqliteService bilibiliSubscribeSqliteService;

    /**
     * 解析指令中的uid，指令不匹配返回null
     */
    protected Long parseUid(Message message, Pattern pattern) {
        String command = this.commandText(message);
        if (StringUtils.isBlank(command)) {
            return null;
        }
        Matcher matcher = pattern.matcher(command);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 指令文本
     * 文本消息只取text段(at机器人、回复引用等cq码会被排除)，其它情况取原始消息
     */
    protected String commandText(Message message) {
        String text = message.isTextMsg() ? message.getText(-1) : message.getRawMessage();
        return Objects.isNull(text) ? null : text.trim();
    }

    /**
     * 查询订阅记录，同一个机器人+同一个主播+同一种订阅类型只有一条
     */
    protected BilibiliSubscribeSqlite findSubscribe(Long uid, Long selfId) {
        if (Objects.isNull(uid) || Objects.isNull(selfId)) {
            return null;
        }
        return bilibiliSubscribeSqliteService.getOne(new LambdaQueryWrapper<BilibiliSubscribeSqlite>()
                .eq(BilibiliSubscribeSqlite::getUid, uid)
                .eq(BilibiliSubscribeSqlite::getSelfId, selfId)
                .eq(BilibiliSubscribeSqlite::getSubType, BilibiliSubscribeTypeEnum.LIVE.getType()), false);
    }

    /**
     * 当前消息所属的机器人qq号
     */
    protected Long selfId(Bot bot, Message message) {
        return Objects.nonNull(message.getSelfId()) ? message.getSelfId() : bot.getId();
    }

    /**
     * 群消息返回群号，私聊返回发消息的人
     */
    protected Long targetId(Message message) {
        return message.isGroupMsg() ? message.getGroupId() : message.getUserId();
    }

    /**
     * 回复消息(群消息回复到群，私聊回复给发消息的人)
     */
    protected void reply(Bot bot, Message message, String text) {
        bot.sendMessage(message.getUserId(), message.getGroupId(), message.getMessageType(),
                MessageHolder.instanceText(text));
    }

    /**
     * 回复中的主播名称，没有昵称时展示uid
     */
    protected String displayName(Long uid, String uname) {
        return StringUtils.isNotBlank(uname) ? StrFormatter.format("{}（{}）", uname, uid) : "uid:" + uid;
    }

    /**
     * 群消息返回"本群"，私聊返回"您"
     */
    protected String selfLabel(Message message) {
        return message.isGroupMsg() ? "本群" : "您";
    }
}
