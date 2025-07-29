package com.haruhi.botServer.thread;

import com.haruhi.botServer.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 该线程用来执行bot刚启动时就需要执行的一些有关业务的处理
 */
@Slf4j
@Component
public class FirstTask {

    private final SystemService systemService;

    public FirstTask(SystemService systemService) {
        this.systemService = systemService;
        execute();
    }

    public synchronized void execute(){
        try {
            systemService.loadCache();
            // 创建stop脚本
            systemService.writeStopScript();
        }catch (Exception e){
            log.error("初始任务执行异常",e);
        }
    }

}
