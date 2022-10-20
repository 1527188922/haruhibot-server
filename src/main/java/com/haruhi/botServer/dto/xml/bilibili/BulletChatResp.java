package com.haruhi.botServer.dto.xml.bilibili;


import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

@Data
@XmlRootElement(name = "i")
public class BulletChatResp implements Serializable {

    private List<String> d;
}
