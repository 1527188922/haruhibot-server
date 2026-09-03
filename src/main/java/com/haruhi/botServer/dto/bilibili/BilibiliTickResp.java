package com.haruhi.botServer.dto.bilibili;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
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

    public Long expires(){
        if (created_at == null || ttl == null) {
            return null;
        }
        return ttl + created_at;
    }

    // true:过期
    public boolean expired() {
        Long expireTime = expires();
        if (expireTime == null) {
            return true;
        }
        long l = DateUtil.currentSeconds();
        // 提前5分钟算过期
        return l > expireTime - (5 * 60);
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class Nav {
        private String img;
        private String sub;
    }
}
