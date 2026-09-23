package com.haruhi.botserver.features.wordstrip.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.features.contacts.model.AvatarInfo;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

@Data
@TableName(value = DataBaseConst.T_WORD_STRIP)
public class WordStripSqlite extends AvatarInfo {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long groupId;
    private Long selfId;
    private String keyWord;
    private String answer;
    private String createTime;
    private String modifyTime;

    @TableField(exist = false)
    private String groupName;


    public Date createTimeParsed(){
        if (StringUtils.isNotBlank(createTime)) {
            return null;
        }
        return DateTimeUtil.parseDate(createTime, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }

    public Date modifyTimeParsed(){
        if (StringUtils.isNotBlank(modifyTime)) {
            return null;
        }
        return DateTimeUtil.parseDate(modifyTime, DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }

}
