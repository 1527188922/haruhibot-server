package com.haruhi.botServer.controller.web;

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

        List<GroupInfoSqlite> groupInfoList = new ArrayList<>();
        if (Objects.nonNull(botId)) {
            Bot botById = BotContainer.getBotById(botId);
            if (botById == null) {
                return HttpResp.fail("机器人QQ不存在或未连接："+botId,null);
            }
            groupInfoList.addAll(groupInfoSqliteService.loadGroupInfo(botById));
        }else{
            Collection<Bot> bots = BotContainer.getBots();
            if (CollectionUtils.isNotEmpty(bots)) {
                List<Bot> collect = bots.stream().filter(e -> Objects.nonNull(e.getId())).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(collect)) {
                    return HttpResp.fail("当前无QQ客户端连接",null);
                }
                for (Bot bot : collect) {
                    groupInfoList.addAll(groupInfoSqliteService.loadGroupInfo(bot));
                }
            }
        }

        List<Map<String,Object>> list = new ArrayList<>();
        groupInfoList.stream()
                .collect(Collectors.groupingBy(GroupInfoSqlite::getSelfId,Collectors.toList()))
                .forEach((k,v)->{
                    Map<String, Object> item = new HashMap<>();
                    item.put("selfId",k);
                    item.put("selfAvatarUrl", CommonUtil.getAvatarUrl(k,false));
                    item.put("groupList",v);
                    List<GroupInfoSqlite> oldGroupInfoList = groupInfoSqliteService.selectBySelfId(k);

                    List<GroupInfoSqlite> added = CommonUtil.findAdded(v, oldGroupInfoList, GroupInfoSqlite::getGroupId);
                    item.put("addedGroupList",added);
                    List<GroupInfoSqlite> removed = CommonUtil.findRemoved(v, oldGroupInfoList, GroupInfoSqlite::getGroupId);
                    item.put("removedGroupList",removed);

                    list.add(item);
                });
        return HttpResp.success("刷新完成",list);
    }
}
