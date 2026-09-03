package com.haruhi.botServer.dto.bilibili;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class BilibiliTickResp {

    private String ticket;
    //秒级时间戳 10位
    private Long created_at;
    // 有效期 单位秒
    private Long ttl;
    private HashMap<String,Object> context;
    private Nav nav;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Nav {
        private String img;
        private String sub;
    }
}
