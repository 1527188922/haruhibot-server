package com.haruhi.botserver.administration.controller;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.shared.annotation.IgnoreAuthentication;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.shared.model.HttpResp;
import com.haruhi.botserver.shared.model.BaseResp;
import com.haruhi.botserver.administration.service.LoginService;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsCloseCodes;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsSessionRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping(SysConstants.CONTEXT_PATH+"/user")
public class UserController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private WebuiWsSessionRegistry webuiWsSessionRegistry;

    @IgnoreAuthentication
    @PostMapping("/login")
    public HttpResp login(@RequestBody JSONObject request) {
        String username = request.getString("username");
        String password = request.getString("password");

        BaseResp<String> resp = loginService.login(username, password);
        if (!BaseResp.SUCCESS_CODE.equals(resp.getCode())) {
            return HttpResp.fail(resp.getMsg(), null);
        }

        request.remove("password");
        request.put("token", resp.getData());
        return HttpResp.success(request);
    }

    @GetMapping("/logout")
    public HttpResp logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = authorization;
        if (StringUtils.isNotBlank(token) && token.startsWith(LoginService.TOKEN_PREFIX)) {
            token = token.substring(LoginService.TOKEN_PREFIX.length());
        }
        // 先取出用户名，再失效token，最后踢掉该用户的webui websocket连接
        String username = loginService.verifyAndRefreshToken(token);
        loginService.logout(token);
        if (StringUtils.isNotBlank(username)) {
            webuiWsSessionRegistry.closeByUsername(username, WebuiWsCloseCodes.LOGOUT, "logout");
        }
        return HttpResp.success();
    }
}
