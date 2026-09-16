package com.haruhi.botServer.service;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.RegisteredPayload;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.haruhi.botServer.dto.BaseResp;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.PropertiesUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;
@Service
public class LoginService {

    public static final String HEADER_KEY_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String JWT_PAYLOAD_USERNAME = "username";

    private final Cache<String, TokenSession> tokenCache;
    private final long jwtExpireHours;
//    private final long graceMinutes;
    private final String jwtSecret;

    public LoginService() {
        int loginExpireMin = Integer.parseInt(
                PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG,
                        PropertiesUtil.PROP_KEY_WEBUI_LOGIN_EXPIRE, "30"));

        int maxSession = Integer.parseInt(
                PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG,
                        PropertiesUtil.PROP_KEY_WEBUI_SESSION_MAX, "6"));

        this.jwtExpireHours = Long.parseLong(
                PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG,
                        PropertiesUtil.PROP_KEY_WEBUI_JWT_EXPIRE, "12"));

//        this.graceMinutes = Long.parseLong(
//                PropertiesUtil.getProperty(FileUtil.FILE_NAME_WEBUI_CONFIG,
//                        PropertiesUtil.PROP_KEY_WEBUI_JWT_GRACE, String.valueOf(loginExpireMin)));

        this.jwtSecret = PropertiesUtil.getProperty(
                FileUtil.FILE_NAME_WEBUI_CONFIG,
                PropertiesUtil.PROP_KEY_WEBUI_JWT_SECRET);

        if (StringUtils.isBlank(jwtSecret)) {
            throw new IllegalStateException("未配置 login.jwt.secret");
        }

        this.tokenCache = Caffeine.newBuilder()
                .expireAfterAccess(loginExpireMin, TimeUnit.MINUTES)
                .maximumSize(maxSession)
                .build();
    }

    public BaseResp<String> login(String username, String password) {
        String loginUserName = PropertiesUtil.getProperty(
                FileUtil.FILE_NAME_WEBUI_CONFIG,
                PropertiesUtil.PROP_KEY_WEBUI_LOGIN_USERNAME);

        String loginUserPassword = PropertiesUtil.getProperty(
                FileUtil.FILE_NAME_WEBUI_CONFIG,
                PropertiesUtil.PROP_KEY_WEBUI_LOGIN_PASSWORD);

        if (StringUtils.isBlank(loginUserName) || StringUtils.isBlank(loginUserPassword)) {
            return BaseResp.fail("未配置webui账户密码");
        }

        if (!loginUserName.equals(username) || !loginUserPassword.equals(password)) {
            return BaseResp.fail("用户名或密码错误");
        }

        long now = System.currentTimeMillis();
        long jwtExpireAt = now + TimeUnit.HOURS.toMillis(jwtExpireHours);

        String token = createToken(username, now, jwtExpireAt);

        tokenCache.put(token, new TokenSession(username, jwtExpireAt, now));

        return BaseResp.success(token);
    }

    private String createToken(String username, long now, long jwtExpireAt) {
        return JWT.create()
                .setPayload(JWT_PAYLOAD_USERNAME, username)
                .setIssuedAt(new Date(now))
                .setExpiresAt(new Date(jwtExpireAt))
                .setKey(jwtSecret.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    /**
     * 校验 JWT，并刷新内存缓存。
     * @return 合法则返回 username，否则返回 null
     */
    public String verifyAndRefreshToken(String token) {
        if (StringUtils.isBlank(token)) {
            return null;
        }

        JWT jwt;
        try {
            jwt = JWT.of(token).setKey(jwtSecret.getBytes(StandardCharsets.UTF_8));
            if (!jwt.verify()) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        String username = (String) jwt.getPayload(JWT_PAYLOAD_USERNAME);
        if (StringUtils.isBlank(username)) {
            return null;
        }
        Number expNumber = (Number) jwt.getPayload(RegisteredPayload.EXPIRES_AT);
        long jwtExpireAt = expNumber == null ? 0 : expNumber.longValue() * 1000L;
        long now = System.currentTimeMillis();

        boolean jwtExpired;
        try {
            JWTValidator.of(jwt).validateDate();
            jwtExpired = false;
        } catch (Exception e) {
            jwtExpired = true;
        }

        // JWT 未过期，直接放行，并刷新内存滑动缓存
        if (!jwtExpired) {
            tokenCache.put(token, new TokenSession(username, jwtExpireAt, now));
            return username;
        }

        // JWT 已过期，看内存缓存是否还在
        TokenSession session = tokenCache.getIfPresent(token);
        if (session == null) {
            return null;
        }

        // 加绝对宽限，防止旧 token 无限续期
//        if (now > session.jwtExpireAt + TimeUnit.MINUTES.toMillis(graceMinutes)) {
//            tokenCache.invalidate(token);
//            return null;
//        }

        // 宽限期内，刷新滑动过期时间
        tokenCache.put(token, session);
        return username;
    }

    public void logout(String token) {
        if (StringUtils.isNotBlank(token)) {
            tokenCache.invalidate(token);
        }
    }

    @AllArgsConstructor
    @Data
    public static class TokenSession {
        private String username;
        private long jwtExpireAt;
        private long createdAt;
    }
}