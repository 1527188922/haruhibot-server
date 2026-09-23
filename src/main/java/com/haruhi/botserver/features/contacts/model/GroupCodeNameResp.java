package com.haruhi.botserver.features.contacts.model;

import com.haruhi.botserver.shared.model.CodeNameResp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 群列表(群号/群名/群头像)响应
 * 单独定义而不是直接在CodeNameResp上加头像字段：CodeNameResp还用于枚举等通用code/name场景，不需要头像
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GroupCodeNameResp extends CodeNameResp {

    /**
     * 群头像地址
     */
    private String avatarUrl;

    public GroupCodeNameResp(Serializable code, String name, String avatarUrl) {
        super(code, name);
        this.avatarUrl = avatarUrl;
    }
}
