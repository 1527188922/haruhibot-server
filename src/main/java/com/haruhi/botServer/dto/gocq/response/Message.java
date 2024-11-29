package com.haruhi.botServer.dto.gocq.response;

import com.haruhi.botServer.constant.event.ElementTypeEnum;
import com.haruhi.botServer.constant.event.MessageHolderTypeEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
                   List<MessageHolder> message, String rawMessage, Integer font, Sender sender, String messageId, Integer messageSeq, String anonymous,
                   Raw raw,Long interval,Status status) {
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
        this.raw = raw;
        this.interval = interval;
        this.status = status;
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
    private final List<MessageHolder> message;
    private final String rawMessage;
    private final Integer font;
    private final Sender sender;
    private final String messageId;
    private final Integer messageSeq;
    private final String anonymous;
    private final Raw raw;
    private final Long interval;
    private final Status status;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Status{
        private Boolean online;
        private Boolean good;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Raw{
        private List<Element> elements;
        private List<Record> records;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageHolder{
        private String type;
        private MessageData data;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageData{
        // image
        private String file; 
        private String url;
        private String fileSize;
        
        // reply
        private String id;
        
        // text
        private String text;
        // at
        private String qq;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Record{
        private String msgId;
        private String msgRandom;
        private Integer chatType;
        private Integer subMsgType;
        private String peerUid;
        private String sendMemberName;
        private List<Element> elements;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Element{
        private Integer elementType;
        private String elementId;
        private PicElement picElement;
        private ReplyElement replyElement;
        private TextElement textElement;
        
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PicElement{
        private String fileName;
        private String sourcePath;
        private String fileSize;
        private Long picWidth;
        private Long picHeight;
        
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReplyElement{
        private String replayMsgId;
        private String replayMsgSeq;
        private String replayMsgRootSeq;
        private String replayMsgRootMsgId;
        private String sourceMsgIdInRecords;
        private String senderUid;
        private String replyMsgTime;

    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TextElement{
        private String content;
        private String atUid;
        private String atTinyId;
        private Integer atType;
        private String atRoleId;
        private String atRoleName;
        private Integer needNotify;

    }
    
    public boolean isGroupMsg(){
        return MessageTypeEnum.group.getType().equals(this.messageType);
    }

    public boolean isPrivateMsg(){
        return MessageTypeEnum.privat.getType().equals(this.messageType);
    }
    
    public boolean isReplyMsg(){
        if(!CollectionUtils.isEmpty(this.message)){
            return this.message.stream().map(MessageHolder::getType).collect(Collectors.toList()).contains(MessageHolderTypeEnum.reply.name());
        }
        return false;
    }

    public boolean isPicMsg(){
        if(!CollectionUtils.isEmpty(this.message)){
            return this.message.stream().map(MessageHolder::getType).collect(Collectors.toList()).contains(MessageHolderTypeEnum.image.name());
        }
        return false;
    }

    public boolean isTextMsg(){
        if(!CollectionUtils.isEmpty(this.message)){
            return this.message.stream().map(MessageHolder::getType).collect(Collectors.toList()).contains(MessageHolderTypeEnum.text.name());
        }
        return false;
    }

    public boolean isTextMsgOnly(){
        if(CollectionUtils.isEmpty(this.message)){
            return false;
        }
        List<MessageHolder> collect = this.message.stream().filter(e -> !MessageHolderTypeEnum.text.name().equals(e.getType())).collect(Collectors.toList());
        return CollectionUtils.isEmpty(collect);
    }

    public boolean isAtMsg(){
        if(!CollectionUtils.isEmpty(this.message)){
            return this.message.stream().map(MessageHolder::getType).collect(Collectors.toList()).contains(MessageHolderTypeEnum.at.name());
        }
        return false;
    }

    public boolean isSelfMsg(){
        return userId != null && userId.equals(selfId);
    }
    /**
     * 用户at 机器人
     * @return
     */
    public boolean isAtBot(){
        return isAtQQ(String.valueOf(this.selfId));
    }

    /**
     * 用户at用户自己
     * @return
     */
    public boolean isAtSelf(){
        return isAtQQ(String.valueOf(this.userId));
    }

    public boolean isAtQQ(String qq){
        return getAtQQs().contains(qq);
    }
    
    public List<String> getTexts(){
        if (isTextMsg()) {
            return this.message.stream().filter(e -> MessageHolderTypeEnum.text.name().equals(e.getType()))
                    .map(MessageHolder::getData)
                    .map(MessageData::getText)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public String getText(int index){
        List<String> texts = getTexts();
        if(CollectionUtils.isEmpty(texts)){
            return null;
        }
        if(index >= 0){
            try {
                return texts.get(index);
            }catch (IndexOutOfBoundsException e){
                return null;
            }
        }
        return StringUtils.join(texts,"");
    }
    
    public List<String> getPicUrls(){
        if(isPicMsg()){
            return this.message.stream().filter(e -> MessageHolderTypeEnum.image.name().equals(e.getType()))
                    .map(MessageHolder::getData)
                    .map(MessageData::getUrl)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public List<MessageData> getPicMessageData(){
        if(isPicMsg()){
            return this.message.stream().filter(e -> MessageHolderTypeEnum.image.name().equals(e.getType()))
                    .map(MessageHolder::getData)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public List<String> getReplyMsgIds(){
        if(isReplyMsg()){
            return this.message.stream().filter(e -> MessageHolderTypeEnum.reply.name().equals(e.getType()))
                    .map(MessageHolder::getData)
                    .map(MessageData::getId)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public List<String> getAtQQs(){
        if(isAtMsg()){
            return this.message.stream().filter(e -> MessageHolderTypeEnum.at.name().equals(e.getType()))
                    .map(MessageHolder::getData)
                    .map(MessageData::getQq)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 获取图片路径
     * 从raw参数中获取图片本地路径
     * @param from 从哪里获取图片路径
     * @return
     */
    public List<String> getPicSourcePath(ElementTypeEnum from){
        if(from == ElementTypeEnum.PIC && isPicMsg()){
            List<PicElement> picElements = raw.getElements().stream().filter(e -> e.getElementType() == ElementTypeEnum.PIC.getType() && e.getPicElement() != null)
                    .map(Element::getPicElement).collect(Collectors.toList());
            return picElements.stream().map(PicElement::getSourcePath).collect(Collectors.toList());
        }
        if(from == ElementTypeEnum.REPLY && isReplyMsg() && !CollectionUtils.isEmpty(this.raw.getRecords())){
            List<String> picSourcePathList = new ArrayList<>();
            for (Record record : this.raw.getRecords()) {
                if(CollectionUtils.isEmpty(record.getElements())){
                    continue;
                }
                // 从records中获取picElements 把picElements中的sourcePath取出来
                List<String> collect = record.getElements().stream().filter(e -> e.getElementType() == ElementTypeEnum.PIC.getType() && e.getPicElement() != null)
                        .map(Element::getPicElement).map(PicElement::getSourcePath).collect(Collectors.toList());
                picSourcePathList.addAll(collect);
            }
            return picSourcePathList;
        }
        return Collections.emptyList();
    }
}
