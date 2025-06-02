package com.haruhi.botServer.controller.web;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.controller.HttpResp;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(BotConfig.CONTEXT_PATH+"/user")
public class UserController {

    @Autowired
    private LoginService loginService;

    @IgnoreAuthentication
    @PostMapping("/login")
    public HttpResp login(@RequestBody JSONObject request) {
        String username = request.getString("username");
        String password = request.getString("password");
        BaseResp<String> resp = loginService.login(username, password);
        if (!BaseResp.SUCCESS_CODE.equals(resp.getCode())) {
            return HttpResp.fail(resp.getMsg(),null);
        }
        request.remove("password");
        request.put("token", resp.getData());
        return HttpResp.success(request);
    }

    @GetMapping("/logout")
    public HttpResp login() {
        loginService.logout(null);
        return HttpResp.success();
    }
}
