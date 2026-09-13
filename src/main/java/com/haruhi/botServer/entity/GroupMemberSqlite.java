package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import com.haruhi.botServer.entity.vo.AvatarInfo;
import lombok.Data;

/**
 * 群成员
 * 数据来源：Bot#getGroupMemberList(Long, long)
 */
@Data
@TableName(value = DataBaseConst.T_GROUP_MEMBER)
public class GroupMemberSqlite extends AvatarInfo {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    // bot qq
    private Long selfId;
    private Long groupId;
    // 群员qq
    private Long userId;
    private String nickname;
    private String card;
    private String sex;
    private Integer age;
    private String area;
    private String level;
    // owner admin member
    private String role;
    private String title;
    private Long joinTime;
    private Long lastSentTime;
    private Integer shutUpTimestamp;
    private Integer titleExpireTime;
    private Boolean unfriendly;
    private Boolean cardChangeable;
    /**
     * 是否已离群
     * 离群的人只标记不删除
     */
    private Boolean leftFlag;
    private String createTime;
    private String modifyTime;

    /**
     * 群名称（查询时关联t_group_info填充，不落库）
     */
    @TableField(exist = false)
    private String groupName;
}
