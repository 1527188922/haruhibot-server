package com.haruhi.botServer.aop;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.druid.DruidConfig;
import com.baomidou.dynamic.datasource.provider.YmlDynamicDataSourceProvider;
import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.LinkedHashMap;

@Component
@Aspect
@Slf4j
public class SqliteDataSourceInitAspect {

    @Before("execution(* com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator.createDataSource(*))")
    public void createDataSourceBefore(JoinPoint joinPoint) throws IOException, SQLException {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        Object arg = args[0];
        if(!(arg instanceof DataSourceProperty)){
            return;
        }
        DataSourceProperty dataSourceProperty = (DataSourceProperty) arg;
        if (!DataBaseConfig.DATA_SOURCE_MASTER.equals(dataSourceProperty.getPoolName())) {
            return;
        }
        String url = dataSourceProperty.getUrl();
        File databaseFile = FileUtil.getSqliteDatabaseFile(url);
        FileUtil.mkdirs(databaseFile.getParent());

        if (databaseFile.exists()) {
            log.info("sqlite db文件路径：{}", databaseFile.getAbsolutePath());
        }else {
            try {
                databaseFile.createNewFile();
                log.info("sqlite db文件创建成功：{}", databaseFile.getAbsolutePath());
            }catch (IOException e){
                log.error("创建sqlite db文件异常",e);
                throw e;
            }
        }
    }


    @Before("execution(* com.baomidou.dynamic.datasource.provider.YmlDynamicDataSourceProvider.loadDataSources())")
    public void loadDataSourcesBefore(JoinPoint joinPoint) throws IllegalAccessException {
        Object target = joinPoint.getTarget();
        if(target instanceof YmlDynamicDataSourceProvider){
            YmlDynamicDataSourceProvider ymlDynamicDataSourceProvider = (YmlDynamicDataSourceProvider) target;
            Field[] dataSourcePropertiesMaps = YmlDynamicDataSourceProvider.class.getDeclaredFields();
            for (Field field : dataSourcePropertiesMaps) {
                field.setAccessible(true);
                Object o = field.get(ymlDynamicDataSourceProvider);
                if(o instanceof LinkedHashMap){
                    LinkedHashMap<String,DataSourceProperty> linkedHashMap = (LinkedHashMap<String,DataSourceProperty>) o;
                    DataSourceProperty masterDataSourceProperty = linkedHashMap.get(DataBaseConfig.DATA_SOURCE_MASTER);
                    if (masterDataSourceProperty == null) {
                        DataSourceProperty dataSourceProperty = new DataSourceProperty();
                        dataSourceProperty.setUrl(DataBaseConfig.SQLITE_DEFAULT_JDBC_URL);
                        dataSourceProperty.setDriverClassName("org.sqlite.JDBC");
                        DruidConfig druid = dataSourceProperty.getDruid();
                        druid.setValidationQuery("SELECT 1");
                        druid.setTestOnBorrow(false);
                        druid.setTestOnReturn(false);
                        druid.setFilters("stat");
                        druid.setPoolPreparedStatements(false);
                        linkedHashMap.put(DataBaseConfig.DATA_SOURCE_MASTER, dataSourceProperty);
                    }
                }
            }
        }
    }
}
