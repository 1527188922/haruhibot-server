package com.haruhi.botServer.aop;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.creator.druid.DruidConfig;
import com.baomidou.dynamic.datasource.provider.YmlDynamicDataSourceProvider;
import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.constant.DataBaseConst;
import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * 数据源初始化
 * <p>
 * <b>数据源的唯一真源是 {@code ./config/database.properties}</b>：
 * 无论 {@code application*.yml} 里怎么写 {@code spring.datasource}，这里都会用配置项的值覆盖 master 数据源。
 * 这样数据源配置和业务配置一样集中在 {@code ./config/} 下、能在配置管理页编辑，
 * 也不会出现"两份配置谁生效"的困惑。
 * <p>
 * 覆盖只作用于 master 数据源，其它数据源（如果以后通过 yml 配置）保持原样。
 */
@Component
@Aspect
@Slf4j
public class SqliteDataSourceInitAspect {

    /**
     * 建连接前确保 sqlite 库文件（及其目录）存在
     */
    @Before("execution(* com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator.createDataSource(*))")
    public void createDataSourceBefore(JoinPoint joinPoint) throws IOException, SQLException {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }
        if (!(args[0] instanceof DataSourceProperty dataSourceProperty)) {
            return;
        }
        if (!DataBaseConst.DATA_SOURCE_MASTER.equals(dataSourceProperty.getPoolName())) {
            return;
        }
        String url = dataSourceProperty.getUrl();
        File databaseFile = FileUtil.getSqliteDatabaseFile(url);
        if (databaseFile == null) {
            // :memory: 之类没有实体文件
            return;
        }
        FileUtil.mkdirs(databaseFile.getParent());

        if (databaseFile.exists()) {
            log.info("sqlite db文件路径：{}", databaseFile.getAbsolutePath());
        } else {
            try {
                databaseFile.createNewFile();
                log.info("sqlite db文件创建成功：{}", databaseFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("创建sqlite db文件异常", e);
                throw e;
            }
        }
    }

    /**
     * 用 database.properties（{@link Configs} 快照）覆盖 master 数据源配置
     */
    @Before("execution(* com.baomidou.dynamic.datasource.provider.YmlDynamicDataSourceProvider.loadDataSources())")
    public void loadDataSourcesBefore(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        if (!(target instanceof YmlDynamicDataSourceProvider provider)) {
            return;
        }
        Map<String, DataSourceProperty> dataSourceMap = reflectDataSourceMap(provider);
        if (dataSourceMap == null) {
            log.warn("未获取到动态数据源配置容器，将只使用 database.properties 中的默认值");
        }

        DataSourceProperty master = dataSourceMap == null ? null : dataSourceMap.get(DataBaseConst.DATA_SOURCE_MASTER);
        boolean fromYml = master != null;
        if (master == null) {
            master = new DataSourceProperty();
        }
        applyDatabaseConfig(master);

        if (dataSourceMap != null) {
            dataSourceMap.put(DataBaseConst.DATA_SOURCE_MASTER, master);
        }
        if (fromYml) {
            log.info("数据源[{}]的 yml 配置已被 {} 覆盖，最终 url：{}", DataBaseConst.DATA_SOURCE_MASTER,
                    ConfigKey.DATABASE_URL.getFile().getFileName(), master.getUrl());
        } else {
            log.info("数据源[{}]由 {} 初始化，url：{}", DataBaseConst.DATA_SOURCE_MASTER,
                    ConfigKey.DATABASE_URL.getFile().getFileName(), master.getUrl());
        }
    }

    /**
     * 从 database.properties 读取并写入数据源属性
     */
    private void applyDatabaseConfig(DataSourceProperty property) {
        property.setUrl(Configs.getStr(ConfigKey.DATABASE_URL));
        property.setDriverClassName(Configs.getStr(ConfigKey.DATABASE_DRIVER_CLASS_NAME));

        DruidConfig druid = property.getDruid();
        if (druid == null) {
            druid = new DruidConfig();
            property.setDruid(druid);
        }
        druid.setValidationQuery(Configs.getStr(ConfigKey.DATABASE_DRUID_VALIDATION_QUERY));
        druid.setFilters(Configs.getStr(ConfigKey.DATABASE_DRUID_FILTERS));
        druid.setTestOnBorrow(Configs.getBool(ConfigKey.DATABASE_DRUID_TEST_ON_BORROW));
        druid.setTestOnReturn(Configs.getBool(ConfigKey.DATABASE_DRUID_TEST_ON_RETURN));
        druid.setPoolPreparedStatements(Configs.getBool(ConfigKey.DATABASE_DRUID_POOL_PREPARED_STATEMENTS));
        druid.setMaxActive(Configs.getInt(ConfigKey.DATABASE_DRUID_MAX_ACTIVE));
    }

    /**
     * YmlDynamicDataSourceProvider 把数据源配置放在私有字段里，这里反射取出来
     */
    @SuppressWarnings("unchecked")
    private Map<String, DataSourceProperty> reflectDataSourceMap(YmlDynamicDataSourceProvider provider) {
        for (java.lang.reflect.Field field : YmlDynamicDataSourceProvider.class.getDeclaredFields()) {
            if (!Map.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(provider);
                if (value instanceof Map<?, ?> map) {
                    return (Map<String, DataSourceProperty>) map;
                }
            } catch (IllegalAccessException e) {
                log.error("读取动态数据源配置失败", e);
            }
        }
        return null;
    }
}
