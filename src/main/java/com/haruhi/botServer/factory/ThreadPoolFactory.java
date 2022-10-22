package com.haruhi.botServer.factory;


import com.haruhi.botServer.utils.system.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadPoolFactory {
    private ThreadPoolFactory(){}
    private final static ThreadPoolExecutor commandHandlerThreadPool = new ThreadPoolExecutor(5,20,36, TimeUnit.HOURS,new ArrayBlockingQueue(120),new CustomizableThreadFactory("pool-handler-"),new ThreadPoolExecutor.CallerRunsPolicy());
    private static Executor downloadThreadPool = null;
    private final static ThreadPoolExecutor chatHistoryThreadPool = new ThreadPoolExecutor(2,10,42, TimeUnit.HOURS,new ArrayBlockingQueue(150),new CustomizableThreadFactory("pool-insertChar-"),new ThreadPoolExecutor.CallerRunsPolicy());
    private final static ThreadPoolExecutor eventThreadPool = new ThreadPoolExecutor(5,16,48, TimeUnit.HOURS,new ArrayBlockingQueue(140),new CustomizableThreadFactory("pool-event-"),new ThreadPoolExecutor.CallerRunsPolicy());

    public static void resetThreadPoolSize(){
        int availableProcessors = SystemInfo.AVAILABLE_PROCESSORS;
        if(availableProcessors > 0){
            commandHandlerThreadPool.setCorePoolSize(availableProcessors + 1);
            commandHandlerThreadPool.setMaximumPoolSize(availableProcessors * 3);

            chatHistoryThreadPool.setCorePoolSize(availableProcessors + 1);
            chatHistoryThreadPool.setMaximumPoolSize(availableProcessors * 4);

            eventThreadPool.setCorePoolSize(availableProcessors + 1);
            eventThreadPool.setMaximumPoolSize(availableProcessors * 4);
            log.info("根据cpu线程数重置线程池容量完成");
        }
    }

    public static Executor getCommandHandlerThreadPool(){
        return commandHandlerThreadPool;
    }

    public synchronized static Executor getDownloadThreadPool(){
        if(downloadThreadPool == null){
            downloadThreadPool = new ThreadPoolExecutor(4,8,60, TimeUnit.SECONDS,new ArrayBlockingQueue(30),new CustomizableThreadFactory("pool-download-"),new ThreadPoolExecutor.CallerRunsPolicy());
        }
        return downloadThreadPool;
    }
    public static Executor getChatHistoryThreadPool(){
        return chatHistoryThreadPool;
    }
    public static Executor getEventThreadPool(){
        return eventThreadPool;
    }
}
