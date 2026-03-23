package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_CHAT_RECORD_EXTEND_V2,autoResultMap = true)
public class ChatRecordExtendV2 {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private Long chatRecordId;
    private Long userId;
    private Long groupId;
    private String rawWsMessage;
    // 数据库类型BLOB 压缩后的二进制raw消息
    private byte[] rawWsMessageBinary;

}
