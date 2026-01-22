//package com.haruhi.botServer.filter;
//
//import jakarta.servlet.*;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//
//@Component
//@Order(1)
//@Slf4j
//public class ConnectRequestFilter implements Filter {
//
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
//            throws IOException, ServletException {
//
//        HttpServletRequest request = (HttpServletRequest) servletRequest;
//        HttpServletResponse response = (HttpServletResponse) servletResponse;
//
//        // 判断请求方法是否为 CONNECT
//        if ("CONNECT".equalsIgnoreCase(request.getMethod())) {
//            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
//            response.setContentType("text/plain;charset=UTF-8");
//            response.getWriter().write("Method CONNECT is not allowed");
//            response.getWriter().flush();
//            response.getWriter().close();
//            log.warn("收到CONNECT请求 IP：{}", request.getRemoteAddr());
//            return;
//        }
//        filterChain.doFilter(request, response);
//    }
//}