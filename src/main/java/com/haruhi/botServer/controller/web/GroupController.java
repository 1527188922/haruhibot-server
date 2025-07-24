package com.haruhi.botServer.controller.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.entity.GroupInfoSqlite;
import com.haruhi.botServer.service.GroupInfoSqliteService;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.BotContainer;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/group")
public class GroupController {

    @Autowired
    private GroupInfoSqliteService groupInfoSqliteService;

    @PostMapping("/search")
    public HttpResp<IPage<GroupInfoSqlite>> search(@RequestBody GroupInfoSqlite request){
        IPage<GroupInfoSqlite> page = groupInfoSqliteService.search(request, false);
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
        return HttpResp.success(groupInfoList);
    }
}
