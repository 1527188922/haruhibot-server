package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class BaseUserInfo {

    @TableField(exist = false)
    private String selfAvatarUrl;
    @TableField(exist = false)
    private String userAvatarUrl;
}
