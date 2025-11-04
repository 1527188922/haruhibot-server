package com.haruhi.botServer.dto.qqclient;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class FriendInfo {
    @JSONField(name = "category_id")
    private Integer categoryId;
    @JSONField(name = "user_id")
    private Long userId;
    private Integer level;
    private String sex;
    private String nickname;
    @JSONField(name = "birthday_day")
    private Integer birthdayDay;
    @JSONField(name = "birthday_month")
    private Integer birthdayMonth;
    @JSONField(name = "birthday_year")
    private Integer birthdayYear;
    @JSONField(name = "phone_num")
    private String phoneNum;
    private String remark;
    private Integer age;
    private String email;

}