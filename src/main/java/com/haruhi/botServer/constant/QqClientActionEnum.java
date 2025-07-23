package com.haruhi.botServer.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum QqClientActionEnum {

    SEND_MSG("send_msg","发送消息"),
    SEND_PRIVATE_MSG("send_private_msg","发送私聊消息"),
    SEND_GROUP_MSG("send_group_msg","发送群消息"),
    SEND_FORWARD_MSG("send_forward_msg","转发合并消息"),
    SEND_GROUP_FORWARD_MSG("send_group_forward_msg","转发群合并消息"),
    SEND_PRIVATE_FORWARD_MSG("send_private_forward_msg","转发私聊合并消息"),
    SEND_LIKE("send_like","发送好友赞"),

    GET_GROUP_MEMBER_LIST("get_group_member_list","获取群成员列表"),
    GET_FORWARD_MSG("get_forward_msg","获取合并转发内容"),
    GET_WORD_SLICES(".get_word_slices","获取中文分词"),
    GET_MSG("get_msg","根据message_id获取消息详情"),
    GET_GROUP_LIST("get_group_list","获取群列表"),
    GET_LOGIN_INGO("get_login_info","获取登录号信息"),
    UPLOAD_PRIVATE_FILE("upload_private_file","上传私聊文件"),
    UPLOAD_GROUP_FILE("upload_group_file","上传群文件"),
    DOWNLOAD_FILE("download_file","下载文件到缓存目录");

    private String action;
    private String remarks;


    public static QqClientActionEnum getActionEnum(String action) {
        for (QqClientActionEnum value : QqClientActionEnum.values()) {
            if (value.action.equals(action)) {
                return value;
            }
        }
        return null;
    }

}
