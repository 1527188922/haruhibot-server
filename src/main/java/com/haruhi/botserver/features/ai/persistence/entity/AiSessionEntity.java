package com.haruhi.botserver.features.ai.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

@Data
@TableName(DataBaseConst.T_AI_SESSION)
public class AiSessionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long selfId;
    private String scene;
    private Long targetId;
    private String roleName;
    private String status;
    private String lastActiveAt;
    private String createdAt;
    private String updatedAt;
}
