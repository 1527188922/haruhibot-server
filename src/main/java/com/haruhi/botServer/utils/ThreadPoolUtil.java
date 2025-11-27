package com.haruhi.botServer.utils;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
public class ThreadPoolUtil {

    @Getter
    private final static ExecutorService handleCommandPool = Executors.newVirtualThreadPerTaskExecutor();

}
