package com.haruhi.botServer.controller;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.dto.qqclient.RequestBox;
import com.haruhi.botServer.dto.qqclient.SyncResponse;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.ws.Bot;
import com.haruhi.botServer.ws.BotContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/bot")
public class BotController {
    

    @IgnoreAuthentication
    @PostMapping("/action/{action}")
    public HttpResp sendMessage(@RequestBody JSONObject params,
                                @RequestParam(value = "botId",required = false) Long botId,
                                @RequestParam(value = "async",required = false) String async,
                                @RequestParam(value = "echo",required = false) String echo,
                                @RequestParam(value = "timeout",required = false,defaultValue = "10000") Long timeout,
                                @PathVariable(value = "action") String action) {
        RequestBox<JSONObject> request = new RequestBox<>();
        request.setAction(action);
        request.setParams(params);
        request.setEcho(echo);

        Bot bot = null;
        if (Objects.isNull(botId)) {
            bot = BotContainer.getBotFirst();
        }else{
            bot = BotContainer.getBotById(botId);
        }
        if (bot == null) {
            return HttpResp.fail("无连接",null);
        }
        if("1".equals(async)){
            // 异步发送
            request.setEcho(null);
            bot.sendMessage(JSONObject.toJSONString(request));
            return HttpResp.success("已发送",null);
        }

        try {
            request.setEcho(StringUtils.isNotBlank(request.getEcho()) ? request.getEcho() : CommonUtil.uuid());
            SyncResponse<Object> syncResponse = bot.sendSyncRequest(JSONObject.toJSONString(request),request.getEcho(), timeout, new TypeReference<SyncResponse<Object>>() {
            });
            return HttpResp.success("发送成功",syncResponse);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            return HttpResp.fail("发送异常",e.getMessage());
        }

    }

}
