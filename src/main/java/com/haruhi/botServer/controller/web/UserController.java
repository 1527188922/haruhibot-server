package com.haruhi.botServer.controller.web;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.WebuiConfig;
import com.haruhi.botServer.controller.HttpResp;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/user")
public class UserController {

    private final WebuiConfig webuiConfig;

    public UserController(WebuiConfig webuiConfig) {
        this.webuiConfig = webuiConfig;
    }

    @PostMapping("/login")
    public HttpResp login(@RequestBody JSONObject request) {
        if(StringUtils.isBlank(webuiConfig.getLoginUserName())
        || StringUtils.isBlank(webuiConfig.getLoginPassword())){
            return HttpResp.fail("未配置webui账户密码",null);
        }
        String username = request.getString("username");
        String password = request.getString("password");
        if(webuiConfig.getLoginUserName().equals(username)
        && webuiConfig.getLoginPassword().equals(password)){
            return HttpResp.success(request);
        }
        return HttpResp.fail("用户名或密码错误",null);
    }
}
