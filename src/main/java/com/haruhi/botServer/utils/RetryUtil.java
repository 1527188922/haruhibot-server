package com.haruhi.botServer.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * 通用重试工具
 */
@Slf4j
public class RetryUtil {

    /**
     * 通用重试
     * @param maxAttempt 总尝试次数（包含第一次执行，例：3=执行1次+最多重试2次）
     * @param sleepMs 基础休眠毫秒
     * @param enableBackOff 是否开启指数退避（sleepMs *=2）
     * @param retryExceptionPredicate 判定该异常是否需要重试；true=需要重试
     * @param resultFailPredicate 业务返回值失败判定，true代表结果不满足，需要重试；传null代表不校验返回值，只看异常
     * @param action 执行逻辑 Callable，允许抛异常
     * @return 成功结果
     * @throws Exception 全部重试耗尽抛出上一次异常
     */
    public static <T> T retry(int maxAttempt,
                              long sleepMs,
                              boolean enableBackOff,
                              Predicate<Exception> retryExceptionPredicate,
                              Predicate<T> resultFailPredicate,
                              Callable<T> action) throws Exception {
        if (maxAttempt < 1) {
            throw new IllegalArgumentException("maxAttempt必须大于0");
        }
        int[] currentRound = new int[]{0};
        Exception lastException = null;
        long currentSleep = sleepMs;

        while (currentRound[0] < maxAttempt) {
            currentRound[0]++;
            try {
                T result = action.call();
                // 判断业务返回值是否失败
                if (resultFailPredicate != null && resultFailPredicate.test(result)) {
                    lastException = new RuntimeException("业务结果判定失败，需要重试");
                    log.warn("[重试]第{}轮执行，业务返回结果不满足预期", currentRound);
                } else {
                    return result;
                }
            } catch (Exception e) {
                lastException = e;
                // 当前异常不允许重试，直接抛出终止
                if (!retryExceptionPredicate.test(e)) {
                    log.error("[重试]捕获不允许重试的异常，直接终止", e);
                    throw e;
                }
                log.warn("[重试]第{}轮执行捕获异常", currentRound, e);
            }

            // 达到最大次数，退出循环抛出异常
            if (currentRound[0] >= maxAttempt) {
                break;
            }
            Thread.sleep(currentSleep);
            if (enableBackOff) {
                currentSleep = currentSleep * 2;
            }
        }
        log.error("[重试]全部{}次尝试均失败", maxAttempt, lastException);
        throw lastException;
    }

    // -------------------- 常用重载：只捕获异常，不校验返回结果 --------------------
    public static <T> T retryOnException(int maxAttempt,
                                         long sleepMs,
                                         boolean enableBackOff,
                                         Predicate<Exception> retryExceptionPredicate,
                                         Callable<T> action) throws Exception {
        return retry(maxAttempt, sleepMs, enableBackOff, retryExceptionPredicate, null, action);
    }

}
