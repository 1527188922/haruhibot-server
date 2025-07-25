package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class GroupInfoQueryReq extends PageReq{

    private String groupName;
    private Long groupId;
    private Long selfId;
}
