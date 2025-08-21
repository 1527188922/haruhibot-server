package com.haruhi.botServer.dto.bilibili;


import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Data
@XmlRootElement(name = "i")
@XmlAccessorType(XmlAccessType.FIELD)
public class BulletChatResp {

    @XmlElement(name = "d")
    private List<Item> itemList;

    // 其他字段根据XML结构添加
    private String chatserver;
    private String chatid;
    private String mission;
    private Long maxlimit;
    private Integer state;
    @XmlElement(name = "real_name")
    private String realName;
    private String source;

    public List<String> getChatList(){
        if (CollectionUtils.isNotEmpty(itemList)) {
            return itemList.stream().map(Item::getContent).filter(StringUtils::isNotBlank).collect(Collectors.toList());
        }
        return null;
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Item {
        @XmlAttribute(name = "p")
        private String p;

        @XmlValue
        private String content;
    }

}
