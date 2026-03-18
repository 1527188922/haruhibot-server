package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import lombok.Data;

@Data
@TableName(DataBaseConst.T_CHAT_RECORD_EXTEND_V2)
public class ChatRecordExtendV2 {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private Long chatRecordId;
    private Long userId;
    private Long groupId;
    private String rawWsMessage;

}
