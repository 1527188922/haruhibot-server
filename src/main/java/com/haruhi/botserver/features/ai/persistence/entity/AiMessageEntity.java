package com.haruhi.botserver.features.ai.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

@Data
@TableName(DataBaseConst.T_AI_MESSAGE)
public class AiMessageEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String role;
    private Long speakerId;
    private String speakerName;
    private String content;
    private String messageId;
    private Integer tokenCount;
    private String createdAt;
}
