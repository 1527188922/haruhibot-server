package com.haruhi.botserver.features.chatrecord.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_CHAT_RECORD_EXTEND_V2,autoResultMap = true)
public class ChatRecordExtendV2 {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private Long chatRecordId;
    private Long userId;
    private Long selfId;
    private String messageType;
    private Long groupId;
    private String rawWsMessage;
    // 数据库类型BLOB 压缩后的二进制raw消息
    private byte[] rawWsMessageBinary;

}
