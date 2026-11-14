package com.haruhi.botserver.integration.onebot.websocket;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.bot.session.BotContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * 机器人反向ws握手拦截器
 * <p>
 * 所有拒绝逻辑都必须放在 beforeHandshake：afterHandshake 时握手已完成，设置状态码无法阻止连接
 */
@Slf4j
@Component
public class BotWsHandshakeInterceptor implements HandshakeInterceptor {

    private final BotWsState botWsState;

    public BotWsHandshakeInterceptor(BotWsState botWsState) {
        this.botWsState = botWsState;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String host = request.getRemoteAddress().getHostString();
        int port = request.getRemoteAddress().getPort();
        log.info("收到握手请求 {}:{}",host,port);
        if (!checkAuthorization(request)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            log.error("无token或token错误，禁止握手！ {}:{}",host,port);
            return false;
        }
        if (!botWsState.isRunning()) {
            response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
            log.info("机器人反向ws已逻辑停止，禁止握手 {}:{}",host,port);
            return false;
        }
        return checkConnections();
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }

    /**
     * 检查当前连接数是否达到上限
     * @return
     */
    private boolean checkConnections(){
        int botMaxConnections = Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS, 0);
        if(botMaxConnections < 0){
            return true;
        }
        int connections = BotContainer.getConnections();
        if(connections >= botMaxConnections){
            log.info("当前连接数:{},已达到最大连接数:{},本次禁止握手",connections, botMaxConnections);
            return false;
        }
        return true;
    }

    /**
     * 检查token
     * @param request
     * @return
     */
    private boolean checkAuthorization(ServerHttpRequest request){
        String botAccessToken = Configs.getStr(ConfigKey.WS_ACCESS_TOKEN, null);
        if (Strings.isBlank(botAccessToken)) {
            // 未配置 则无需认证 直接通过
            return true;
        }
        HttpHeaders headers = request.getHeaders();
        List<String> authorization = headers.get("Authorization");
        if (CollectionUtils.isEmpty(authorization)) {
            return false;
        }
        boolean hasToken = false;
        for (String s : authorization) {
            if(s.contains("Token " + botAccessToken)){
                hasToken = true;
                break;
            }
        }
        return hasToken;
    }
}
