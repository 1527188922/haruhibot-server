package com.haruhi.botServer.utils;


import com.haruhi.botServer.thread.pool.HandleCommandThreadPoolExecutor;
import com.haruhi.botServer.thread.pool.policy.ShareRunsPolicy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.*;

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
            new ShareRunsPolicy());

    @Getter
    private final static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(2);

}
