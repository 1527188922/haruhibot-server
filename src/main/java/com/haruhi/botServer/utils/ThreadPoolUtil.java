package com.haruhi.botServer.utils;


import com.haruhi.botServer.thread.pool.HandleCommandThreadPoolExecutor;
import com.haruhi.botServer.thread.pool.policy.ShareRunsPolicy;
import com.haruhi.botServer.utils.system.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadPoolUtil {
    private ThreadPoolUtil(){}
    private final static ThreadPoolExecutor handleCommandPool = new HandleCommandThreadPoolExecutor(5, 10, 1, TimeUnit.HOURS,
            new ArrayBlockingQueue(20), new CustomizableThreadFactory("pool-handleCommand-"), new ShareRunsPolicy("pool-handleCommand"));

    private final static ExecutorService sharePool = new ThreadPoolExecutor(1, 1,3L * 1000L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),new CustomizableThreadFactory("pool-share-"));

    public static void resetThreadPoolSize(){
        int availableProcessors = SystemInfo.AVAILABLE_PROCESSORS;
        if(availableProcessors > 0){
            handleCommandPool.setCorePoolSize(availableProcessors + 1);
            handleCommandPool.setMaximumPoolSize(availableProcessors * 2);

            log.info("根据cpu线程数:{},重置命令处理线程池容量完成",availableProcessors);
        }
    }

    public static Executor getHandleCommandPool(){
        return handleCommandPool;
    }

    public static ExecutorService getSharePool(){
        return sharePool;
    }

}
