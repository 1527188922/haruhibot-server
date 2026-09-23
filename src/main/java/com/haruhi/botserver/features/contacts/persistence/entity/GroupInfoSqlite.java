package com.haruhi.botserver.features.contacts.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.features.contacts.model.AvatarInfo;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_GROUP_INFO)
public class GroupInfoSqlite extends AvatarInfo {

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

}
