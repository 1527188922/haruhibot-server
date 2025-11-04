package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_FRIEND)
public class FriendSqlite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;

    private Long selfId;

    /**
     * 好友用户ID
     */
    private Long userId;

    /**
     * 生日-日
     */
    private Integer birthdayDay;

    /**
     * 生日-年
     */
    private Integer birthdayYear;

    /**
     * 生日-月
     */
    private Integer birthdayMonth;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 好友等级
     */
    private Integer level;

    /**
     * 性别
     */
    private String sex;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 电话号码
     */
    private String phoneNum;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 邮箱
     */
    private String email;
}