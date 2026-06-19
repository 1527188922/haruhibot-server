package com.haruhi.botServer.controller.web;

import cn.hutool.core.lang.mutable.MutablePair;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.vo.CodeNameReq;
import com.haruhi.botServer.vo.CodeNameResp;
import com.haruhi.botServer.vo.HttpResp;
import com.haruhi.botServer.entity.GroupInfoSqlite;
import com.haruhi.botServer.service.GroupInfoSqliteService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.vo.GroupInfoQueryReq;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.BotContainer;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/group")
public class GroupController {

    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;


    @PostMapping("/list")
    public HttpResp<List<CodeNameResp>> list(@RequestBody CodeNameReq request){
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
}
