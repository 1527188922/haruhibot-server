package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@TableName(value = DataBaseConfig.T_CHAT_RECORD)
public class ChatRecord {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String card;
    private String nickname;
    private String messageId;
    private Long groupId;
    private Long userId;
    private Long selfId;
    private String content;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;
    private Boolean deleted;
    
    private String messageType;
    
    @TableField(exist = false)
    private Long total;

    @TableField(exist = false)
    private String userAvatarUrl;
    @TableField(exist = false)
    private String selfAvatarUrl;
}
