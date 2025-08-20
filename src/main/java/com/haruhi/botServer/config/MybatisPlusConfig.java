package com.haruhi.botServer.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.SQLITE);
        // 可选配置：
        // 溢出总页数后是否进行处理（默认false，即返回最后一页）
        paginationInterceptor.setOverflow(true);
        // 单页最大限制数量（默认无限制）
//        paginationInterceptor.setMaxLimit(500L);

        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}

