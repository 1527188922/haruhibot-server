package com.haruhi.botServer.thread;

import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.WordCloudUtil;
import com.haruhi.botServer.utils.system.SystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * 用于分词的线程
 * 生成词料（之后还要设置权重）
 */
@Slf4j
public class WordSlicesTask implements Callable<List<String>> {
    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    public static int poolSize = availableProcessors + 1;
    public static final ExecutorService pool = new ThreadPoolExecutor(poolSize, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),new CustomizableThreadFactory("pool-word-slices-"));
    private List<String> data;

    public WordSlicesTask(List<String> data){
        this.data = data;
    }

    @Override
    public List<String> call() throws Exception {
        List<String> res = new ArrayList<>(data.size() * 2);
        for (String item : data) {
            List<String> strings = WordCloudUtil.mmsegWordSlices(item);
            if(CollectionUtils.isEmpty(strings)){
                continue;
            }
            res.addAll(strings);
        }
        return res;
    }

    /**
     * 执行分词方法
     * 先根据线程池大小分组
     * 每个句子根据正则去除字符串
     * 再分词
     * @param corpus 未做任何处理的原句子
     * @return
     */
    public static List<String> execute(List<String> corpus){
        List<String> strings = new ArrayList<>(corpus.size() * 3);
        // 根据线程池大小，计算每个线程需要跑几个词语 确保不会有线程空闲
        int limit = CommonUtil.averageAssignNum(corpus.size(),poolSize);
        List<List<String>> lists = CommonUtil.averageAssignList(corpus, limit);
        List<FutureTask<List<String>>> futureTasks = new ArrayList<>(lists.size());
        for (List<String> list : lists) {
            FutureTask<List<String>> listFutureTask = new FutureTask<>(new WordSlicesTask(list));
            futureTasks.add(listFutureTask);
            pool.submit(listFutureTask);
        }
        try {
            for (FutureTask<List<String>> futureTask : futureTasks) {
                strings.addAll(futureTask.get());
            }
            return strings;
        }catch (Exception e){
            log.error("获取分词结果异常",e);
            return null;
        }

    }
}
