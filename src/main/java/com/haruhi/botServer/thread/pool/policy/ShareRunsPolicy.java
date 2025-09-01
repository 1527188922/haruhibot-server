package com.haruhi.botServer.thread.pool.policy;

import com.haruhi.botServer.utils.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class ShareRunsPolicy implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (!executor.isShutdown()) {
            ThreadPoolUtil.getFixedThreadPool().execute(r);
        }

    }
}
