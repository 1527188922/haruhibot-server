package com.haruhi.botServer.test;

import com.haruhi.botServer.config.DataBaseConfig;
import com.haruhi.botServer.entity.PokeReply;
import com.haruhi.botServer.mapper.PokeReplyMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

@Slf4j
@Component
public class TransactionTest implements ISpringTester {

    @Override
    public boolean enable() {
        return false;
    }

    @Autowired
    private PokeReplyMapper pokeReplyMapper;

    @Override
    public void test() {
//        test1();
        test2();
    }

    @Autowired
    private PlatformTransactionManager platformTransactionManager;// 具体类型 JdbcTransactionManager 该对象含有DynamicRoutingDataSource

    /**
     * springboot+mybatis编程式事务
     * platformTransactionManager中已经存在数据源：DynamicRoutingDataSource 该数据源为mybatisplus提供
     */
    public void test1(){
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            PokeReply pokeReply = new PokeReply();
            pokeReply.setReply("test");
            pokeReplyMapper.insert(pokeReply);
            int n = 10/0;
            PokeReply pokeReply2 = new PokeReply();
            pokeReply2.setReply("hello world");
            pokeReplyMapper.insert(pokeReply2);
            platformTransactionManager.commit(transactionStatus);
            log.info("事务提交");
        }catch (Exception e){
            log.error("sql执行异常",e);
            platformTransactionManager.rollback(transactionStatus);
        }

    }

    /**
     * JdbcTemplate 编程式事务
     * 数据源为 HikariDataSource（springboot默认）
     * dynamicRoutingDataSource 也是一个数据源 可以直接作为new JdbcTemplate和new DataSourceTransactionManager的构造参数传入
     * JdbcTemplate和JdbcTransactionManager 这两者要使用同一个数据源，事务才能生效
     */
    public void test2()  {
        JdbcTransactionManager jdbcTransactionManager = null;
        if(platformTransactionManager instanceof JdbcTransactionManager){
            jdbcTransactionManager = (JdbcTransactionManager)platformTransactionManager;
        }else{
            HikariDataSource hikariDataSource = new HikariDataSource();
            hikariDataSource.setJdbcUrl(DataBaseConfig.JDBC_URL);
            hikariDataSource.setUsername(DataBaseConfig.DATA_BASE_BOT_USERNAME);
            hikariDataSource.setPassword(DataBaseConfig.DATA_BASE_BOT_PASSWORD);
            jdbcTransactionManager = new JdbcTransactionManager(hikariDataSource);
        }
        DataSource dataSource = jdbcTransactionManager.getDataSource();
        //定义一个数据源 可以new一个其他数据源

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        PlatformTransactionManager platformTransactionManager = new DataSourceTransactionManager(dataSource);
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        TransactionStatus transactionStatus = platformTransactionManager.getTransaction(transactionDefinition);
        try {
            jdbcTemplate.update("INSERT INTO t_poke_reply ( reply ) VALUES ( ? ) ", "test1-1");
            jdbcTemplate.update("INSERT INTO t_poke_reply ( reply ) VALUES ( ? ) ", "hello world");
            int n = 10/0;
            platformTransactionManager.commit(transactionStatus);
        } catch (Exception e) {
            log.error("sql执行异常",e);
            //6.回滚事务：platformTransactionManager.rollback
            platformTransactionManager.rollback(transactionStatus);
        }
    }
}
