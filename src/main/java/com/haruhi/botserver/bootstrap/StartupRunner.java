package com.haruhi.botserver.bootstrap;

import com.haruhi.botserver.shared.constant.BusinessModuleEnum;
import com.haruhi.botserver.administration.service.SystemService;
import com.haruhi.botserver.infrastructure.logging.DbLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


/**
 * 该线程用来执行bot刚启动时就需要执行的一些有关业务的处理
 */
@Slf4j
@Component
public class StartupRunner implements CommandLineRunner {

    @Autowired
    private SystemService systemService;

    public synchronized void execute(){
        try {
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
