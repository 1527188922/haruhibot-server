package com.haruhi.botServer.service;

import com.haruhi.botServer.cache.CacheMap;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.PropertiesUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LoginService {

    public static final String HEADER_KEY_USER_NAME = "UserName";
    public static final String HEADER_KEY_AUTHORIZATION = "Authorization";

    public static final TimeUnit LOGIN_TIMEOUT_UNIT = TimeUnit.MINUTES;

    public final CacheMap<String,String> WEB_TOKEN_CACHE;

    public LoginService() {
        String loginExpire = PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG, PropertiesUtil.PROP_KEY_WEBUI_LOGIN_EXPIRE,"30");
        this.WEB_TOKEN_CACHE = new CacheMap<>(Long.parseLong(loginExpire), LOGIN_TIMEOUT_UNIT,1);
    }

    public BaseResp<String> login(String username, String password) {

        String loginUserName = PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG, PropertiesUtil.PROP_KEY_WEBUI_LOGIN_USERNAME);
        String loginUserPassword = PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG, PropertiesUtil.PROP_KEY_WEBUI_LOGIN_PASSWORD);

        if(StringUtils.isBlank(loginUserName)
                || StringUtils.isBlank(loginUserPassword)){
            return BaseResp.fail("未配置webui账户密码");
        }
        if(loginUserName.equals(username)
                && loginUserPassword.equals(password)){
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
        String loginUserName = PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG, PropertiesUtil.PROP_KEY_WEBUI_LOGIN_USERNAME);
        if (!userName.equals(loginUserName)) {
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
