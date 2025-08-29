package com.haruhi.botServer.utils;


import com.haruhi.botServer.thread.pool.HandleCommandThreadPoolExecutor;
import com.haruhi.botServer.thread.pool.policy.ShareRunsPolicy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThreadPoolUtil {

    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();

    @Getter
    private final static HandleCommandThreadPoolExecutor handleCommandPool = new HandleCommandThreadPoolExecutor(
            availableProcessors,
            availableProcessors * 2 + 1,
            1,
            TimeUnit.HOURS,
            new ArrayBlockingQueue<>(200),
            new CustomizableThreadFactory("pool-handleCommand-"),
            new ShareRunsPolicy("pool-handleCommand"));

    @Getter
    private final static ExecutorService sharePool = new ThreadPoolExecutor(1, 1,3L * 1000L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),new CustomizableThreadFactory("pool-share-"));


    @Getter
    private final static ExecutorService commonExecutor = new ThreadPoolExecutor(
            availableProcessors * 2 + 1,
            availableProcessors * 4 + 1,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200),
            new CustomizableThreadFactory("common-"));

}
