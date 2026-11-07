package com.haruhi.botserver.integration.onebot.model.event;

import com.haruhi.botserver.integration.onebot.model.MessageTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 通知事件
 * 文档：https://napneko.github.io/onebot/event#通知事件
 * 不同 notice_type 的字段差异很大，这里全部声明为可选字段，
 * 避免像 bot_offline 这类事件因为 message 字段类型不一致导致整体解析失败
 */
@Data
@NoArgsConstructor
public class NoticeEvent extends BaseEvent {

    /**
     * 通知类型
     */
    private String noticeType;
    /**
     * 通知子类型
     */
    private String subType;
    private Long groupId;
    private Long userId;
    /**
     * 操作人id 比如群管理员a踢了一个人,那么该值为a的qq号
     */
    private Long operatorId;
    /**
     * 被操作人/被戳人id
     */
    private Long targetId;
    private Long senderId;
    private Long messageId;
    /**
     * 禁言时长（秒）
     */
    private Integer duration;
    /**
     * 文本内容（bot_offline 等事件的 message 是字符串）
     */
    private String message;
    private String tag;
    private String cardNew;
    private String cardOld;
    private String nameNew;
    private String title;
    /**
     * 群荣誉类型（honor 事件）
     */
    private String honorType;
    /**
     * 群文件上传事件的文件信息
     */
    private GroupUploadFile file;
    /**
     * 群消息表情回应
     */
    private List<MsgEmojiLike> likes;

    public boolean isGroupNotice() {
        return groupId != null;
    }

    public String getMessageType() {
        return isGroupNotice() ? MessageTypeEnum.group.getType() : MessageTypeEnum.privat.getType();
    }

    public boolean isSelfMsg() {
        return userId != null && userId.equals(getSelfId());
    }

    @Data
    @NoArgsConstructor
    public static class GroupUploadFile implements Serializable {
        private String id;
        private String name;
        private Long size;
        private String url;
        private Long busid;
    }

    @Data
    @NoArgsConstructor
    public static class MsgEmojiLike implements Serializable {
        private String emojiId;
        private Integer count;
    }
}
