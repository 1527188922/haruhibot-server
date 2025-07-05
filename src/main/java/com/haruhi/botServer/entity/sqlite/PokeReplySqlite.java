package com.haruhi.botServer.entity.sqlite;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;

@Data
@TableName(value = DataBaseConfig.T_POKE_REPLY)
public class PokeReplySqlite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String reply;
}
