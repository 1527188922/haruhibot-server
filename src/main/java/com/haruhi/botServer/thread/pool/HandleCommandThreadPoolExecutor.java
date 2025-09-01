package com.haruhi.botServer.thread.pool;


import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HandleCommandThreadPoolExecutor extends ThreadPoolExecutor {

    public HandleCommandThreadPoolExecutor(int corePoolSize,
                                           int maximumPoolSize,
                                           long keepAliveTime,
                                           @NotNull TimeUnit unit,
                                           @NotNull BlockingQueue<Runnable> workQueue,
                                           @NotNull ThreadFactory threadFactory,
                                           @NotNull RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }


    @Override
    public void execute(@NotNull Runnable command) {
        execute(command,true);
    }

    public void execute(@NotNull Runnable command,boolean printLog) {
        super.execute(command);
        if(printLog){
            log.info("线程池已受理一个命令:{}",command);
        }
    }

}
