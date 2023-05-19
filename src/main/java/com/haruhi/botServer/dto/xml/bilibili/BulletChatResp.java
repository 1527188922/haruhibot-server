package com.haruhi.botServer.dto.xml.bilibili;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BulletChatResp implements Serializable {

    private List<String> d;
}
