package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.utils.FileUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;

@Data
@TableName(value = DataBaseConfig.T_CUSTOM_REPLY)
public class CustomReplySqlite {


    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String regex;
    private String reply;
    private Integer deleted;

    private Integer isText;// 是否为文本消息 若true，则取reply字段直接发送文本回复
    private String cqType;
    private String relativePath;// 优先 相对路径 父级目录为：程序目录/replyCustom
    private String url;//文件url
    private String groupIds;// 为空：群聊情况下 所有群可触发
    private String messageType;// 为空：群聊私聊都可触发


    public String getAbsolutePath(){
        if(1==isText || StringUtils.isBlank(relativePath)){
            return null;
        }
        return relativePath.startsWith(File.separator) ? FileUtil.getCustomReplyDir() + relativePath
                : FileUtil.getCustomReplyDir() + File.separator + relativePath;
    }


    public boolean pass(String msgType, String groupId){
        if(StringUtils.isBlank(msgType)){
            return false;
        }
        if (StringUtils.isNotBlank(messageType) && !messageType.equals(msgType)) {
            return false;
        }
        return pass(msgType, this.groupIds, groupId);
    }

    private boolean pass(String msgType, String groupIds, String groupId){
        if (MessageTypeEnum.privat.getType().equals(msgType)) {
            return true;
        }
        if (MessageTypeEnum.group.getType().equals(msgType)) {
            if(StringUtils.isBlank(groupIds)){
                return true;
            }
            return Arrays.asList(groupIds.split(",")).contains(groupId);
        }
        return false;
    }
}
