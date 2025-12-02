package com.haruhi.botServer.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class CreateTmpDirectoryFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        File tmp = (File)request.getServletContext().getAttribute(ServletContext.TEMPDIR);
        if (!tmp.exists()) {
            tmp.mkdirs();
            log.info("创建了临时目录：{}",tmp);
        }
        filterChain.doFilter(request,response);
    }
}
