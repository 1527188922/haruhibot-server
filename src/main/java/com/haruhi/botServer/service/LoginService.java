package com.haruhi.botServer.service;

import com.haruhi.botServer.cache.CacheMap;
import com.haruhi.botServer.config.WebuiConfig;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.utils.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginService {

    @Autowired
    private WebuiConfig webuiConfig;

    public static final String HEADER_KEY_USER_NAME = "UserName";
    public static final String HEADER_KEY_AUTHORIZATION = "Authorization";

    public static final CacheMap<String,String> WEB_TOKEN_CACHE = new CacheMap<>(5, TimeUnit.MINUTES,1);


    public BaseResp<String> login(String username, String password) {
        if(StringUtils.isBlank(webuiConfig.getLoginUserName())
                || StringUtils.isBlank(webuiConfig.getLoginPassword())){
            return BaseResp.fail("未配置webui账户密码");
        }
        if(webuiConfig.getLoginUserName().equals(username)
                && webuiConfig.getLoginPassword().equals(password)){
            String token = CommonUtil.uuid();
            refreshWebToken(username, token);
            return BaseResp.success(token);
        }
        return BaseResp.fail("用户名或密码错误");
    }

    public boolean verifyWebToken(String userName,String token){
        if(StringUtils.isBlank(userName) || StringUtils.isBlank(token)){
            return false;
        }
        if (!userName.equals(webuiConfig.getLoginUserName())) {
            return false;
        }
        String s = WEB_TOKEN_CACHE.get(userName);
        return token.equals(s);
    }


    public void refreshWebToken(String userName,String token){
        WEB_TOKEN_CACHE.removeAll();
        WEB_TOKEN_CACHE.put(userName, token);
    }

    public void logout(String userName){
        WEB_TOKEN_CACHE.removeAll();
    }
}
