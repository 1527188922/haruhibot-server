package com.haruhi.botserver.features.ai.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

@Data
@TableName(DataBaseConst.T_AI_MEMORY)
public class AiMemoryEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private String memKey;
    private String memValue;
    private String createdAt;
    private String updatedAt;
}
