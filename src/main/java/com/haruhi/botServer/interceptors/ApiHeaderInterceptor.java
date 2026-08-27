package com.haruhi.botServer.interceptors;

import cn.hutool.extra.servlet.JakartaServletUtil;
import com.haruhi.botServer.annotation.BusinessModule;
import com.haruhi.botServer.annotation.IgnoreAuthentication;
import com.haruhi.botServer.service.LoginService;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.DbLog;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * webui http请求头拦截
 * 拦截路径：/api/**
 */
@Slf4j
@Component
public class ApiHeaderInterceptor implements HandlerInterceptor {
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_REQUEST_METHOD = "requestMethod";
    private static final String MDC_REQUEST_URI = "requestUri";
    private static final String MDC_QUERY_STRING = "queryString";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_USER_NAME = "userName";
    private static final String MDC_HANDLER_CLASS = "handlerClass";
    private static final String MDC_HANDLER_METHOD = "handlerMethod";
    private static final String[] HTTP_MDC_KEYS = {
            MDC_TRACE_ID,
            DbLog.MDC_KEY_BUSINESS_MODULE,
            MDC_REQUEST_METHOD,
            MDC_REQUEST_URI,
            MDC_QUERY_STRING,
            MDC_CLIENT_IP,
            MDC_USER_NAME,
            MDC_HANDLER_CLASS,
            MDC_HANDLER_METHOD
    };

    @Autowired
    private LoginService loginService;
//    private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        startTimeThreadLocal.set(System.currentTimeMillis());
//        log.info("访问api=[{}] IP=[{}]", request.getRequestURI(),request.getRemoteAddr());

        HandlerMethod handlerMethod = null;
        if(handler instanceof HandlerMethod){
            handlerMethod = (HandlerMethod)handler;
        }
        String userName = request.getHeader(LoginService.HEADER_KEY_USER_NAME);
        fillHttpMdc(request, handlerMethod, userName);
        if(handlerMethod == null){
            return true;
        }

        IgnoreAuthentication ignoreAuthentication = handlerMethod.getMethodAnnotation(IgnoreAuthentication.class);
        if(ignoreAuthentication != null){
            return true;
        }
        String token = request.getHeader(LoginService.HEADER_KEY_AUTHORIZATION);

        if (!loginService.verifyWebToken(userName,token)) {
            log.error("非法请求api=[{}] IP=[{}] UserCode：{} Authorization：{}",
                    request.getRequestURI(),request.getRemoteAddr(),userName,token);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
//            response.getWriter().print(JSONObject.toJSONString(HttpResp.fail(401,"认证异常",null)));
            return false;
        }
        loginService.refreshWebToken(userName, token);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        for (String key : HTTP_MDC_KEYS) {
            MDC.remove(key);
        }
    }

    private void fillHttpMdc(HttpServletRequest request, HandlerMethod handlerMethod, String userName) {
        MDC.put(MDC_TRACE_ID, CommonUtil.uuid());
        MDC.put(MDC_REQUEST_METHOD, request.getMethod());
        MDC.put(MDC_REQUEST_URI, request.getRequestURI());
        if (StringUtils.isNotBlank(request.getQueryString())) {
            MDC.put(MDC_QUERY_STRING, request.getQueryString());
        }
        MDC.put(MDC_CLIENT_IP, JakartaServletUtil.getClientIP(request));
        if (StringUtils.isNotBlank(userName)) {
            MDC.put(MDC_USER_NAME, userName);
        }
        if (handlerMethod == null) {
            return;
        }
        MDC.put(MDC_HANDLER_CLASS, handlerMethod.getBeanType().getName());
        MDC.put(MDC_HANDLER_METHOD, handlerMethod.getMethod().getName());
        String businessModule = resolveBusinessModule(handlerMethod);
        if (StringUtils.isNotBlank(businessModule)) {
            MDC.put(DbLog.MDC_KEY_BUSINESS_MODULE, businessModule);
        }
    }

    private String resolveBusinessModule(HandlerMethod handlerMethod) {
        BusinessModule methodAnnotation = handlerMethod.getMethodAnnotation(BusinessModule.class);
        if (methodAnnotation != null && StringUtils.isNotBlank(methodAnnotation.value())) {
            return methodAnnotation.value();
        }
        BusinessModule classAnnotation = handlerMethod.getBeanType().getAnnotation(BusinessModule.class);
        if (classAnnotation != null && StringUtils.isNotBlank(classAnnotation.value())) {
            return classAnnotation.value();
        }
        return handlerMethod.getBeanType().getSimpleName();
    }
}
