package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;

@Data
@TableName(value = DataBaseConfig.T_GROUP_INFO)
public class GroupInfoSqlite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    // bot qq
    private Long selfId;
    private Long groupId;
    private String groupName;
    private Integer memberCount;
    private Integer maxMemberCount;
    private Integer groupAllShut;
    private String groupRemark;


    private String groupMemo;
    private String groupCreateTime;
    private Integer groupLevel;

    @TableField(exist = false)
    private String selfAvatarUrl;
}
