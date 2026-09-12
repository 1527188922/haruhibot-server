package com.haruhi.botServer.dto.qqclient;


import com.haruhi.botServer.constant.event.MessageHolderTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageHolder{
    private String type;
    private MessageData data;

    public static List<MessageHolder> instanceText(String ...texts){
        List<MessageHolder> messageHolders = new ArrayList<>();
        for (String s : texts) {
            MessageData messageData = new MessageData();
            messageData.setText(s);
            messageHolders.add(new MessageHolder(MessageHolderTypeEnum.text.name(),messageData));
        }
        return messageHolders;
    }

    public static List<MessageHolder> instanceReply(String ...ids){
        List<MessageHolder> messageHolders = new ArrayList<>();
        for (String s : ids) {
            MessageData messageData = new MessageData();
            messageData.setId(s);
            messageHolders.add(new MessageHolder(MessageHolderTypeEnum.reply.name(),messageData));
        }
        return messageHolders;
    }


    public static MessageHolder instanceImage(String file){
        MessageData messageData = new MessageData();
        messageData.setFile(file);
        return new MessageHolder(MessageHolderTypeEnum.image.name(),messageData);
    }

    public static MessageHolder instanceRecord(String file){
        MessageData messageData = new MessageData();
        messageData.setFile(file);
        return new MessageHolder(MessageHolderTypeEnum.record.name(),messageData);
    }


    public static MessageHolder instanceVideo(String file){
        MessageData messageData = new MessageData();
        messageData.setFile(file);
        return new MessageHolder(MessageHolderTypeEnum.video.name(),messageData);
    }

    public static List<MessageHolder> instanceAt(String ...qqs){
        List<MessageHolder> messageHolders = new ArrayList<>();
        for (String s : qqs) {
            MessageData messageData = new MessageData();
            messageData.setQq(s);
            messageHolders.add(new MessageHolder(MessageHolderTypeEnum.at.name(),messageData));
        }
        return messageHolders;
    }

    public static List<MessageHolder> instanceAtAll(){
        List<MessageHolder> messageHolders = new ArrayList<>();
        MessageData messageData = new MessageData();
        messageData.setQq("all");
        messageHolders.add(new MessageHolder(MessageHolderTypeEnum.at.name(),messageData));
        return messageHolders;
    }

    public static MessageHolder instanceJson(String json){
        MessageData messageData = new MessageData();
        messageData.setData(json);
        return new MessageHolder(MessageHolderTypeEnum.json.name(),messageData);
    }


    /**
     *
     * @param songId
     * @param platform qq 163 kugou migu kuwo custom
     * @param url 跳转url
     * @param image 封面
     * @return
     */
    public static List<MessageHolder> instanceMusic(String songId, String platform, String url, String image){
        List<MessageHolder> messageHolders = new ArrayList<>();
        MessageData messageData = new MessageData();
        messageData.setId(songId);
        messageData.setType(platform);
        messageData.setUrl(url);
        messageData.setImage(image);
        messageHolders.add(new MessageHolder(MessageHolderTypeEnum.music.name(), messageData));
        return messageHolders;
    }

}