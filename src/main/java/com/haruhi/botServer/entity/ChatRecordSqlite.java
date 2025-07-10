package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.utils.DateTimeUtil;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = DataBaseConfig.T_CHAT_RECORD)
public class ChatRecordSqlite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String card;
    private String nickname;
    private String messageId;
    private Long groupId;
    private Long userId;
    private Long selfId;
    private String content;
    private String time;
    private Integer deleted;

    private String messageType;

    @TableField(exist = false)
    private Long total;

    @TableField(exist = false)
    private String userAvatarUrl;
    @TableField(exist = false)
    private String selfAvatarUrl;


    public Date timeParsed(){
        return DateTimeUtil.parseDate(time, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }

}
