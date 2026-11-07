package com.haruhi.botserver.integration.onebot.model;

import com.haruhi.botserver.integration.onebot.model.event.BaseEvent;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息事件
 * 文档：https://napneko.github.io/onebot/event#消息事件
 */
@Getter
@Setter
public class Message extends BaseEvent implements Serializable {

    /**
     * group/private，缺省时根据 groupId/userId 推导
     */
    private String messageType;
    /**
     * normal/anonymous/notice 等
     */
    private String subType;
    private Long userId;
    private Long groupId;
    /**
     * 私聊消息接收方id（message_sent 等场景）
     */
    private Long targetId;
    private List<MessageHolder> message;
    private String rawMessage;
    private Sender sender;
    private String messageId;

    //非qq客户端接口协议字段
    private String rawWsMsg;

    public String getMessageType() {
        if (StringUtils.isBlank(messageType)) {
            if (groupId != null) {
                return MessageTypeEnum.group.getType();
            }
            if (userId != null) {
                return MessageTypeEnum.privat.getType();
            }
        }
        return messageType;
    }

    public boolean isGroupMsg(){
        return MessageTypeEnum.group.getType().equals(getMessageType());
    }

    public boolean isPrivateMsg(){
        return MessageTypeEnum.privat.getType().equals(getMessageType());
    }

    public boolean isReplyMsg(){
        if(!CollectionUtils.isEmpty(this.message)){
            return this.message.stream().map(MessageHolder::getType).collect(Collectors.toList()).contains(MessageHolderTypeEnum.reply.name());
        }
        return false;
    }

    public boolean isPicMsg(){
        return isMsg(MessageHolderTypeEnum.image);
    }

    public boolean isPicMsgOnly(){
        return isOnly(MessageHolderTypeEnum.image);
    }

    public boolean isTextMsg(){
        return isMsg(MessageHolderTypeEnum.text);
    }

    public boolean isTextMsgOnly(){
        return isOnly(MessageHolderTypeEnum.text);
    }

    public boolean isAtMsg(){
        return isMsg(MessageHolderTypeEnum.at);
    }

    public boolean isJsonMsg(){
        return isMsg(MessageHolderTypeEnum.json);
    }

    public boolean isMsg(MessageHolderTypeEnum holderTypeEnum){
        if(!CollectionUtils.isEmpty(this.message)){
            return this.message.stream().map(MessageHolder::getType).collect(Collectors.toList()).contains(holderTypeEnum.name());
        }
        return false;
    }

    public boolean isOnly(MessageHolderTypeEnum holderTypeEnum){
        if(CollectionUtils.isEmpty(this.message)){
            return false;
        }
        List<MessageHolder> collect = this.message.stream().filter(e -> !holderTypeEnum.name().equals(e.getType())).collect(Collectors.toList());
        return CollectionUtils.isEmpty(collect);
    }

    public boolean isSelfMsg(){
        return userId != null && userId.equals(getSelfId());
    }
    /**
     * 用户at 机器人
     * @return
     */
    public boolean isAtBot(){
        return isAtQQ(String.valueOf(getSelfId()));
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

    public List<String> getJsons(){
        if(isJsonMsg()){
            return this.message.stream().filter(e -> MessageHolderTypeEnum.json.name().equals(e.getType()))
                    .map(MessageHolder::getData)
                    .map(MessageData::getData)
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
}
