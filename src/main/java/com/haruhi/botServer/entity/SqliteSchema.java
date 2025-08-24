package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import lombok.Data;

@Data
@TableName(value = DataBaseConst.SQLITE_SYS_T_SQLITE_SCHEMA)
public class SqliteSchema {
    private String type;
    private String name;
    private String tblName;
    private Integer rootpage;
    private String sql;
}
