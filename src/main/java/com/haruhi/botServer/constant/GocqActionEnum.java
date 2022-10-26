package com.haruhi.botServer.constant;

public enum GocqActionEnum {

    SEND_MSG("send_msg","发送消息"),
    SEND_PRIVATE_MSG("send_private_msg","发送私聊消息"),
    SEND_GROUP_MSG("send_group_msg","发送群消息"),
    SEND_GROUP_FORWARD_MSG("send_group_forward_msg","转发群合并消息"),
    SEND_PRIVATE_FORWARD_MSG("send_private_forward_msg","转发私聊合并消息"),

    GET_GROUP_MEMBER_LIST("get_group_member_list","获取群成员列表"),
    GET_FORWARD_MSG("get_forward_msg","获取合并转发内容"),
    GET_WORD_SLICES(".get_word_slices","获取中文分词"),
    GET_MSG("get_msg","根据message_id获取消息详情"),
    GET_GROUP_LIST("get_group_list","获取群列表"),
    GET_LOGIN_INGO("get_login_info","获取登录号信息");

    private String action;
    private String remarks;
    GocqActionEnum(String action, String remarks){
        this.action = action;
        this.remarks = remarks;
    }
    public String getAction(){
        return action;
    }
    public String getRemarks(){
        return remarks;
    }
}
