package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = DataBaseConfig.T_SEND_LIKE_RECORD)
public class SendLikeRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Long userId;
    private Long selfId;
    private Date sendTime;
    private Integer times;
    private String messageType;
}
