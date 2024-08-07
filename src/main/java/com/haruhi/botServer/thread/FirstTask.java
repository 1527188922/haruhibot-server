package com.haruhi.botServer.thread;

import com.haruhi.botServer.service.SystemService;
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

    public synchronized void execute(){
        new Thread(() -> {
            try {
                systemService.loadCache();
                // 创建stop脚本
                systemService.writeStopScript();
            }catch (Exception e){
                log.error("初始任务执行异常",e);
            }
        }).start();
    }

    @Override
    public void run(String... args) throws Exception {
        execute();
    }
    
    
}
