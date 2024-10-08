package com.haruhi.botServer.filter;

import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
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
        File appTempDir = new File(FileUtil.getAppTempDir());
        if(!appTempDir.exists()){
            File file = FileUtil.mkdirs(FileUtil.getAppTempDir());
            log.info("创建了自定义临时目录：{}",file);
        }
        filterChain.doFilter(request,response);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
