package com.haruhi.botServer.aop;

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
@Aspect
@Slf4j
public class SqliteDatabaseInitAspect {

    @Before("execution(* com.baomidou.dynamic.datasource.DynamicDataSourceCreator.createDataSource(*))")
    public void checkDatabaseBeforeMethod(JoinPoint joinPoint) throws IOException {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object arg = args[0];
        if(!(arg instanceof DataSourceProperty)){
            return;
        }
        DataSourceProperty dataSourceProperty = (DataSourceProperty) arg;
        if (!DataBaseConfig.DATA_SOURCE_MASTER.equals(dataSourceProperty.getPollName())) {
            return;
        }
        File file = new File(FileUtil.getSqliteDatabaseFile());
        FileUtil.mkdirs(file.getParent());

        if (!file.exists()) {
            try {
                file.createNewFile();
                log.info("sqlite db文件创建成功：{}", file.getAbsolutePath());
            }catch (IOException e){
                log.error("创建sqlite db文件异常",e);
                throw e;
            }
        }
    }
}
