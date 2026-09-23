package com.haruhi.botserver.features.reply.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_POKE_REPLY)
public class PokeReplySqlite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String reply;
}
