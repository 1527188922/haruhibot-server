package com.haruhi.botServer.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前连接的机器人信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotInfoResp implements Serializable {

    /**
     * 机器人qq号
     */
    private Long id;

    /**
     * 机器人昵称
     */
    private String name;

    /**
     * 头像地址
     */
    private String avatarUrl;
}
