package com.haruhi.botServer.dto.gocq.response;

import com.haruhi.botServer.constant.event.MessageTypeEnum;
import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

import java.io.Serializable;

@Getter
public class Message implements Serializable {

//    @JSONField(name = "post_type")
//    private String postType;
//    @JSONField(name = "meta_event_type")
//    private String metaEventType;
//    @JSONField(name = "message_type")
//    private String messageType;
//    @JSONField(name = "notice_type")
//    private String noticeType;
//    // 操作人id 比如群管理员a踢了一个人,那么该值为a的qq号
//    @JSONField(name = "operator_id")
//    private Long operatorId;
//    private Long time;
//    @JSONField(name = "self_id")
//    private Long selfId;
//    @JSONField(name = "sub_type")
//    private String subType;
//    @JSONField(name = "user_id")
//    private Long userId;
//    @JSONField(name = "sender_id")
//    private Long senderId;
//    @JSONField(name = "group_id")
//    private Long groupId;
//    @JSONField(name = "target_id")
//    private Long targetId;
//    private String message;
//    @JSONField(name = "raw_message")
//    private String rawMessage;
//    private Integer font;
//    private Sender sender;
//    @JSONField(name = "message_id")
//    private String messageId;
//    @JSONField(name = "message_seq")
//    private Integer messageSeq;
//    private String anonymous;

    public Message(String postType, String metaEventType, String messageType, String noticeType, Long operatorId,
                   Long time, Long selfId, String subType, Long userId, Long senderId, Long groupId, Long targetId,
                   String message, String rawMessage, Integer font, Sender sender, String messageId, Integer messageSeq, String anonymous) {
        this.postType = postType;
        this.metaEventType = metaEventType;
        this.noticeType = noticeType;
        this.operatorId = operatorId;
        this.time = time;
        this.selfId = selfId;
        this.subType = subType;
        this.userId = userId;
        this.senderId = senderId;
        this.groupId = groupId;
        this.targetId = targetId;
        this.message = message;
        this.rawMessage = rawMessage;
        this.font = font;
        this.sender = sender;
        this.messageId = messageId;
        this.messageSeq = messageSeq;
        this.anonymous = anonymous;
        this.messageType = messageType;
        if(Strings.isBlank(this.messageType)){
            if(groupId != null){
                this.messageType = MessageTypeEnum.group.getType();
            }else if(this.userId != null){
                this.messageType = MessageTypeEnum.privat.getType();
            }
        }
    }

    private final String postType;
    private final String metaEventType;
    private String messageType;
    private final String noticeType;
    // 操作人id 比如群管理员a踢了一个人,那么该值为a的qq号
    private final Long operatorId;
    private final Long time;
    private final Long selfId;
    private final String subType;
    private final Long userId;
    private final Long senderId;
    private final Long groupId;
    private final Long targetId;
    private final String message;
    private final String rawMessage;
    private final Integer font;
    private final Sender sender;
    private final String messageId;
    private final Integer messageSeq;
    private final String anonymous;
}
