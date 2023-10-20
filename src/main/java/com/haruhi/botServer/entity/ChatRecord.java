package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;

@Data
@TableName(value = DataBaseConfig.T_CHAT_RECORD)
public class ChatRecord {

    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;
    private String card;
    private String nickname;
    private String messageId;
    private Long groupId;
    private Long userId;
    private Long selfId;
    private String content;
    private Long createTime;
    private Boolean deleted;
    
    private String messageType;
    
    @TableField(exist = false)
    private Long total;
}
