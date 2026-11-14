package com.haruhi.botserver.features.ai.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

@Data
@TableName(DataBaseConst.T_AI_USAGE)
public class AiUsageEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer cacheHitTokens;
    private Integer latencyMs;
    private Integer toolCalls;
    private Integer success;
    private String errorCode;
    private String createdAt;
}
