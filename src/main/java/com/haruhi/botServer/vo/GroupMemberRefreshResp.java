package com.haruhi.botServer.vo;

import com.haruhi.botServer.entity.GroupMemberSqlite;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 群成员刷新结果
 */
@Data
public class GroupMemberRefreshResp {

    private Long selfId;
    private Long groupId;
    /**
     * 本次从qq客户端获取到的群成员数量
     */
    private Integer memberCount = 0;
    /**
     * 本次新入库的群员
     */
    private List<GroupMemberSqlite> addedList = new ArrayList<>();
    /**
     * 本次新标记为离群的群员（只标记不删除）
     */
    private List<GroupMemberSqlite> leftList = new ArrayList<>();
    /**
     * 之前已离群、本次又出现在群成员列表中的群员
     */
    private List<GroupMemberSqlite> rejoinList = new ArrayList<>();
}
