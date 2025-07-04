package com.haruhi.botServer.mapper.system;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.haruhi.botServer.config.DataBaseConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
@DS(value = DataBaseConfig.DATA_SOURCE_MYSQL)
public interface DataBaseInitMapper {

    /**
     * 判断数据库是否存在
     * 0不存在 1存在
     * @param dbName
     * @return
     */
    int dataBaseIsExist(@Param("dbName") String dbName);

    /**
     * 创建数据库
     * @param dbName
     * @return
     */
    int createDataBase(@Param("dbName") String dbName);

    /**
     * 判断表是否存在，存在则返回1，不存在则返回0
     */
    int tableIsExist(@Param("dbName") String dbName, @Param("tableName") String tableName);
}
