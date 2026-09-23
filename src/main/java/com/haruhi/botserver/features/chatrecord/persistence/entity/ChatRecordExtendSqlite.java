package com.haruhi.botserver.features.chatrecord.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = DataBaseConst.T_CHAT_RECORD_EXTEND)
public class ChatRecordExtendSqlite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private Long chatRecordId;
    private String messageId;
    private String rawWsMessage;
    private String time;

    public Date timeParsed(){
        return DateTimeUtil.parseDate(time, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }
}
