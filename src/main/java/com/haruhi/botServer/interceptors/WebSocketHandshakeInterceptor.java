package com.haruhi.botServer.interceptors;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.service.DictionarySqliteService;
import com.haruhi.botServer.ws.BotContainer;
import com.haruhi.botServer.ws.BotServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * 拦截握手
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    @Autowired
    private DictionarySqliteService dictionarySqliteService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        log.info("收到握手请求 {}:{}",request.getRemoteAddress().getHostString(),request.getRemoteAddress().getPort());
        if (!checkBotServerRunning(wsHandler)) {
            return false;
        }
        return checkConnections();
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if(!checkAuthorization(request)){
            doForbidden(request,response);
        }
    }

    private void doForbidden(ServerHttpRequest request,ServerHttpResponse response){
        response.setStatusCode(HttpStatus.FORBIDDEN);
        log.error("无token或token错误，禁止握手！ {}:{}",request.getRemoteAddress().getHostString(),request.getRemoteAddress().getPort());
    }

    /**
     * 检查当前连接数是否达到上限
     * @return
     */
    private boolean checkConnections(){
        int botMaxConnections = dictionarySqliteService.getBotMaxConnections();
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


    private boolean checkBotServerRunning(WebSocketHandler wsHandler){
        if(wsHandler instanceof ExceptionWebSocketHandlerDecorator) {
            ExceptionWebSocketHandlerDecorator exceptionWebSocketHandlerDecorator = (ExceptionWebSocketHandlerDecorator) wsHandler;
            WebSocketHandler exceptionWebSocketHandlerDecoratorDelegate = exceptionWebSocketHandlerDecorator.getDelegate();

            if(exceptionWebSocketHandlerDecoratorDelegate instanceof LoggingWebSocketHandlerDecorator) {
                LoggingWebSocketHandlerDecorator loggingWebSocketHandlerDecorator = (LoggingWebSocketHandlerDecorator) exceptionWebSocketHandlerDecoratorDelegate;
                WebSocketHandler loggingWebSocketHandlerDecoratorDelegate = loggingWebSocketHandlerDecorator.getDelegate();
                if(loggingWebSocketHandlerDecoratorDelegate instanceof BotServer){
                    BotServer botServer = (BotServer) loggingWebSocketHandlerDecoratorDelegate;
                    return botServer.isRunning();
                }
            }
        }
        return true;
    }

    /**
     * 检查token
     * @param request
     * @return
     */
    private boolean checkAuthorization(ServerHttpRequest request){
        String botAccessToken = dictionarySqliteService.getBotAccessToken();
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
