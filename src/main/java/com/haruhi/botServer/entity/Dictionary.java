package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = DataBaseConfig.T_DICTIONARY)
public class Dictionary {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String key;
    private String content;
    private Date createTime;
    private Date modifyTime;
}
