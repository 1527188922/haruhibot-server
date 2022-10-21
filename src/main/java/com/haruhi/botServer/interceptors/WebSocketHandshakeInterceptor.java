package com.haruhi.botServer.interceptors;

import com.haruhi.botServer.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * 拦截握手
 */
@Slf4j
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.info("收到握手请求 {}:{}",request.getRemoteAddress().getHostString(),request.getRemoteAddress().getPort());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (Strings.isNotBlank(BotConfig.ACCESS_TOKEN)) {
            HttpHeaders headers = request.getHeaders();
            List<String> authorization = headers.get("Authorization");
            if (CollectionUtils.isEmpty(authorization)) {
                doForbidden(request,response);
                return;
            }
            boolean hasToken = false;
            for (String s : authorization) {
                if(s.contains("Token " + BotConfig.ACCESS_TOKEN)){
                    hasToken = true;
                    break;
                }
            }

            if (!hasToken) {
                doForbidden(request,response);
            }
        }
    }

    private void doForbidden(ServerHttpRequest request,ServerHttpResponse response){
        response.setStatusCode(HttpStatus.FORBIDDEN);
        log.error("无token或token错误，禁止握手！ {}:{}",request.getRemoteAddress().getHostString(),request.getRemoteAddress().getPort());
    }
}
