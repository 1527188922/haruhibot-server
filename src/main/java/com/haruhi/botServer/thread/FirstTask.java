package com.haruhi.botServer.thread;

import com.haruhi.botServer.config.service.ConfigMigrator;
import com.haruhi.botServer.constant.BusinessModuleEnum;
import com.haruhi.botServer.service.SystemService;
import com.haruhi.botServer.utils.DbLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * 该线程用来执行bot刚启动时就需要执行的一些有关业务的处理
 */
@Slf4j
@Component
public class FirstTask implements CommandLineRunner {

    @Autowired
    private SystemService systemService;
    @Autowired
    private ConfigMigrator configMigrator;

    public synchronized void execute(){
        try {
            // 旧版本配置存在数据库里，启动时搬到 ./config/*.properties（幂等，已存在则跳过）
            configMigrator.migrate();
            systemService.loadCache(1);
            // 创建stop脚本
//            systemService.writeStopScript();
        }catch (Exception e){
            DbLog.error(BusinessModuleEnum.SYSTEM,"初始任务执行异常",e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        execute();
    }
    
    
}
