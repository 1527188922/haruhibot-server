package com.haruhi.botServer.interceptors;

import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * webui http请求头拦截
 * 拦截路径：/api/**
 */
@Slf4j
@Component
public class ApiHeaderInterceptor implements HandlerInterceptor {
    @Autowired
    private LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HandlerMethod handlerMethod = null;
        if(handler instanceof HandlerMethod){
            handlerMethod = (HandlerMethod)handler;
        }
        if(handlerMethod == null){
            return true;
        }

        IgnoreAuthentication ignoreAuthentication = handlerMethod.getMethodAnnotation(IgnoreAuthentication.class);
        if(ignoreAuthentication != null){
            return true;
        }
        String token = request.getHeader(LoginService.HEADER_KEY_AUTHORIZATION);
        String userName = request.getHeader(LoginService.HEADER_KEY_USER_NAME);

        if (!loginService.verifyWebToken(userName,token)) {
            log.error("非法请求 Authorization：{} UserCode：{} URL：{}",token,userName,request.getRequestURI());
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//            response.getWriter().print(JSONObject.toJSONString(HttpResp.fail(401,"认证异常",null)));
            return false;
        }
        log.info("访问api=[{}] IP=[{}]", request.getRequestURI(),request.getRemoteAddr());
        loginService.refreshWebToken(userName, token);
        return true;
    }
}
