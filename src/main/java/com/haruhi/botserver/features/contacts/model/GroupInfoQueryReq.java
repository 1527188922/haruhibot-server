package com.haruhi.botserver.features.contacts.model;

import com.haruhi.botserver.shared.model.PageReq;

import lombok.Data;

@Data
public class GroupInfoQueryReq extends PageReq{

    private String groupName;
    private Long groupId;
    private Long selfId;
}
