package com.haruhi.botServer.filter;

import com.haruhi.botServer.config.BotConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Vue History 模式请求转发过滤器
 */
@Component
@Order(2)
public class VueHistoryFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String requestURI = request.getRequestURI();
        String requestMethod = request.getMethod();

        // true:需要转发
        boolean need = HttpMethod.GET.name().equals(requestMethod)
                        && !requestURI.startsWith(BotConfig.CONTEXT_PATH)
                        && !requestURI.contains(".")
                        && !"/index.html".equals(requestURI)
                        && !requestURI.startsWith(BotConfig.DRUID_PATH);

        if (need) {
            // 符合转发规则 → 服务器内部转发到 index.html
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}