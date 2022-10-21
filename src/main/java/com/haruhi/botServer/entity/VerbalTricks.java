package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;

@Data
@TableName(value = DataBaseConfig.T_VERBAL_TRICKS)
public class VerbalTricks {

    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;
    private String regex;
    private String answer;
}
