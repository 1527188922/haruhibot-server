package com.haruhi.botserver.infrastructure.web.websocket;

import com.haruhi.botserver.administration.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * webui WebSocket 握手拦截器
 * <p>
 * 浏览器 WebSocket API 不能自定义请求头，所以登录token只能通过查询参数传递：/api/webui/ws?token=xxx
 * <p>
 * 注意：MVC 的 ApiHeaderInterceptor 不会作用到 WebSocket 握手上，鉴权必须在这里做
 */
@Slf4j
@Component
public class WebuiWsHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 握手通过后写入的登录用户名
     */
    public static final String ATTR_USERNAME = "webuiWsUsername";
    private static final String QUERY_TOKEN = "token";

    private final LoginService loginService;

    public WebuiWsHandshakeInterceptor(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst(QUERY_TOKEN);
        String username = loginService.verifyAndRefreshToken(token);
        if (StringUtils.isBlank(username)) {
            // 不打印token本身，避免日志泄露
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("webui websocket 握手被拒绝，登录态无效 host:{}", request.getRemoteAddress());
            return false;
        }
        attributes.put(ATTR_USERNAME, username);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
