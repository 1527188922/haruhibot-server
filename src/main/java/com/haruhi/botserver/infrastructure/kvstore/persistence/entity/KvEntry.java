package com.haruhi.botserver.infrastructure.kvstore.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = DataBaseConst.T_DICTIONARY)
public class KvEntry {

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
