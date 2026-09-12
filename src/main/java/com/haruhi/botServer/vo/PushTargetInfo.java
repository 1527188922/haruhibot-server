package com.haruhi.botServer.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 推送目标(群/好友)展示信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PushTargetInfo implements Serializable {

    /**
     * 群号/qq号
     */
    private Long id;
    /**
     * 群名称/好友昵称，查不到时为null
     */
    private String name;
    /**
     * 头像地址
     */
    private String avatarUrl;
    /**
     * 是否在t_group_info/t_friend中查询到，false表示未加群/未添加好友
     */
    private Boolean found = false;
}
