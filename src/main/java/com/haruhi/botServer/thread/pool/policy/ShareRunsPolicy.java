package com.haruhi.botServer.thread.pool.policy;

import com.haruhi.botServer.utils.ThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class ShareRunsPolicy implements RejectedExecutionHandler {
    private String poolName;
    public ShareRunsPolicy(String poolName){
        this.poolName = poolName;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        ThreadPoolUtil.getSharePool().execute(r);
        log.info("线程池：{}执行拒绝策略，本次任务由公共线程池执行,executor: {},",poolName,executor.toString());
    }
}
