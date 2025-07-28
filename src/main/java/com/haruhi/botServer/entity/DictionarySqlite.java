package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.utils.DateTimeUtil;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = DataBaseConfig.T_DICTIONARY)
public class DictionarySqlite {

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String key;
    private String content;
    private String remark;
    private String createTime;
    private String modifyTime;

    public Date createTimeParsed(){
        return DateTimeUtil.parseDate(createTime, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }

    public Date modifyParsed(){
        return DateTimeUtil.parseDate(modifyTime, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }
}
