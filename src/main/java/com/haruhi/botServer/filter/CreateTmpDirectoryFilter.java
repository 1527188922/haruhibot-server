package com.haruhi.botServer.filter;

import com.haruhi.botServer.config.path.AbstractPathConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class CreateTmpDirectoryFilter implements Filter {

    @Autowired
    private AbstractPathConfig abstractPathConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        File tmp = (File)request.getServletContext().getAttribute(ServletContext.TEMPDIR);
        if (!tmp.exists()) {
            tmp.mkdirs();
            log.info("创建了临时目录：{}",tmp);
        }
        File multipartFileTemp = abstractPathConfig.tempPath();
        if (!multipartFileTemp.exists()) {
            multipartFileTemp.mkdirs();
            log.info("创建了自定义临时目录：{}",multipartFileTemp);
        }

        filterChain.doFilter(request,response);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
