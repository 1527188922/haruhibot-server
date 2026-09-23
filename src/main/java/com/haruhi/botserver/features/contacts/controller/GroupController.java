package com.haruhi.botserver.features.contacts.controller;

import cn.hutool.core.lang.mutable.MutablePair;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.shared.model.CodeNameReq;
import com.haruhi.botserver.features.contacts.model.GroupCodeNameResp;
import com.haruhi.botserver.shared.model.HttpResp;
import com.haruhi.botserver.features.contacts.persistence.entity.GroupInfoSqlite;
import com.haruhi.botserver.features.contacts.persistence.entity.GroupMemberSqlite;
import com.haruhi.botserver.features.contacts.service.GroupInfoSqliteService;
import com.haruhi.botserver.features.contacts.service.GroupMemberSqliteService;
import com.haruhi.botserver.shared.util.CommonUtil;
import com.haruhi.botserver.features.contacts.model.GroupInfoQueryReq;
import com.haruhi.botserver.features.contacts.model.GroupMemberQueryReq;
import com.haruhi.botserver.features.contacts.model.GroupMemberRefreshResp;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.bot.session.BotContainer;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping(SysConstants.CONTEXT_PATH+"/group")
public class GroupController {

    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;

    @Autowired
    private GroupMemberSqliteService groupMemberSqliteService;


    /**
     * 群列表(群号/群名/群头像)，供群号选择组件远程搜索使用
     */
    @PostMapping("/list")
    public HttpResp<List<GroupCodeNameResp>> list(@RequestBody CodeNameReq request){
        return HttpResp.success(groupInfoSqliteService.codeNameList(request));
    }

    @PostMapping("/search")
    public HttpResp<IPage<GroupInfoSqlite>> search(@RequestBody GroupInfoQueryReq request){
        IPage<GroupInfoSqlite> page = groupInfoSqliteService.search(request, true);
        return HttpResp.success(page);
    }

    @PostMapping("/refresh")
    public HttpResp refresh(@RequestParam(value = "botId",required = false) Long botId){

        List<MutablePair<List<GroupInfoSqlite>,List<GroupInfoSqlite>>> groupInfoList = new ArrayList<>();
        if (Objects.nonNull(botId)) {
            Bot botById = BotContainer.getBotById(botId);
            if (botById == null) {
                return HttpResp.fail("机器人QQ不存在或未连接："+botId,null);
            }
            groupInfoList.add(groupInfoSqliteService.loadGroupInfo(botById));
        }else{
            Collection<Bot> bots = BotContainer.getBots();
            if (CollectionUtils.isNotEmpty(bots)) {
                List<Bot> collect = bots.stream().filter(e -> Objects.nonNull(e.getId())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(collect)) {
                    return HttpResp.fail("当前无QQ客户端连接",null);
                }
                for (Bot bot : collect) {
                    groupInfoList.add(groupInfoSqliteService.loadGroupInfo(bot));
                }
            }
        }

        List<Map<String,Object>> list = new ArrayList<>();
        groupInfoList
                .forEach(mu->{
                    List<GroupInfoSqlite> newList = mu.getKey();
                    List<GroupInfoSqlite> oldList = mu.getValue();
                    if (newList.isEmpty()) {
                        return;
                    }
                    Map<String, Object> item = new HashMap<>();
                    Long selfId = newList.getFirst().getSelfId();
                    item.put("selfId", selfId);
                    item.put("selfAvatarUrl", CommonUtil.getAvatarUrl(selfId,false));
                    item.put("groupList",newList);

                    List<GroupInfoSqlite> added = CommonUtil.findAdded(newList, oldList, GroupInfoSqlite::getGroupId);
                    item.put("addedGroupList",added);
                    List<GroupInfoSqlite> removed = CommonUtil.findRemoved(newList, oldList, GroupInfoSqlite::getGroupId);
                    item.put("removedGroupList",removed);

                    list.add(item);
                });
        return HttpResp.success("刷新完成",list);
    }

    @PostMapping("/member/search")
    public HttpResp<IPage<GroupMemberSqlite>> searchMember(@RequestBody GroupMemberQueryReq request){
        return HttpResp.success(groupMemberSqliteService.search(request));
    }

    /**
     * 刷新指定群的群成员
     * 最新数据中不存在的群员标记为离群（只标记不删除）
     */
    @PostMapping("/member/refresh")
    public HttpResp<GroupMemberRefreshResp> refreshMember(@RequestParam(value = "botId",required = false) Long botId,
                                                          @RequestParam(value = "groupId",required = false) Long groupId){
        if (Objects.isNull(botId) || Objects.isNull(groupId)) {
            return HttpResp.fail("缺少机器人QQ或群号",null);
        }
        Bot bot = BotContainer.getBotById(botId);
        if (bot == null) {
            return HttpResp.fail("机器人QQ不存在或未连接："+botId,null);
        }
        GroupMemberRefreshResp resp = groupMemberSqliteService.refreshGroupMember(bot, groupId, MEMBER_REFRESH_TIMEOUT);
        return HttpResp.success("刷新完成",resp);
    }

    /**
     * 获取群成员超时时间
     */
    private static final long MEMBER_REFRESH_TIMEOUT = Duration.ofSeconds(30).toMillis();
}
