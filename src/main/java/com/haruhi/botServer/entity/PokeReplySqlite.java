package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.T_POKE_REPLY)
public class PokeReplySqlite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String reply;
}
