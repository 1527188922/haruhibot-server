package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import lombok.Data;

@Data
@TableName(value = DataBaseConfig.T_WORD_STRIP)
public class WordStrip {
    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long groupId;
    private Long selfId;
    private String keyWord;
    private String answer;


    @TableField(exist = false)
    private String userAvatarUrl;
    @TableField(exist = false)
    private String selfAvatarUrl;

}
