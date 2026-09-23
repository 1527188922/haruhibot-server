package com.haruhi.botserver.infrastructure.persistence.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
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
