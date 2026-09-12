package com.haruhi.botServer.vo;

import com.haruhi.botServer.constant.PushTargetTypeEnum;
import lombok.Data;

import java.util.List;

/**
 * 推送目标(群/好友)查询
 */
@Data
public class PushTargetQueryReq extends PageReq {

    /**
     * 机器人qq号，用于筛选该机器人已加入的群/已添加的好友
     */
    private Long selfId;

    /**
     * 目标类型 {@link PushTargetTypeEnum}
     */
    private String type;

    /**
     * 群号/群名 或 qq号/昵称
     */
    private String keyword;

    /**
     * 指定要查询的目标id，不为空时忽略keyword，只返回这些id的信息
     */
    private List<Long> ids;
}
