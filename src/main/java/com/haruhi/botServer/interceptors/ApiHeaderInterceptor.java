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
    private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        startTimeThreadLocal.set(System.currentTimeMillis());
        log.info("访问api=[{}] IP=[{}]", request.getRequestURI(),request.getRemoteAddr());
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
            log.error("非法请求api=[{}] IP=[{}] UserCode：{} Authorization：{}",
                    request.getRequestURI(),request.getRemoteAddr(),userName,token);
            response.setContentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//            response.getWriter().print(JSONObject.toJSONString(HttpResp.fail(401,"认证异常",null)));
            return false;
        }
        loginService.refreshWebToken(userName, token);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        long cost = System.currentTimeMillis() - startTimeThreadLocal.get();
        startTimeThreadLocal.remove();

        log.info("访问api=[{}] IP=[{}] cost=[{}ms]", request.getRequestURI(),request.getRemoteAddr(),cost);

    }
}
