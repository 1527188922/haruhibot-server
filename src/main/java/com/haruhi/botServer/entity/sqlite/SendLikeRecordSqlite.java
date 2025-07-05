package com.haruhi.botServer.entity.sqlite;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.utils.DateTimeUtil;
import lombok.Data;

import java.util.Date;


@Data
@TableName(value = DataBaseConfig.T_SEND_LIKE_RECORD)
public class SendLikeRecordSqlite {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long selfId;
    private String sendTime;
    private Integer times;
    private String messageType;


    public Date sendTimeParsed(){
        return DateTimeUtil.parseDate(sendTime, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }

}
