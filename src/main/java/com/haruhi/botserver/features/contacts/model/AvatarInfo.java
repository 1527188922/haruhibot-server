package com.haruhi.botserver.features.contacts.model;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data
public class AvatarInfo {
    @TableField(exist = false)
    private String userAvatarUrl;
    @TableField(exist = false)
    private String targetAvatarUrl;
    @TableField(exist = false)
    private String selfAvatarUrl;
    @TableField(exist = false)
    private String groupAvatarUrl;
}
