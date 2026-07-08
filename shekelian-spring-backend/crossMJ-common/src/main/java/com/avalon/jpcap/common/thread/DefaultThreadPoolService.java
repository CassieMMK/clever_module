package com.avalon.jpcap.common.thread;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 默认线程池
 */
@Slf4j
@Service("defaultThreadPoolService")
public class DefaultThreadPoolService implements ThreadPoolService, InitializingBean {

    private ThreadPoolExecutor threadPoolExecutor;

    @Value("${default.threadPool.coreThreadNum}")
    private int coreThreadNum;

    @Value("${default.threadPool.maxThreadNum}")
    private int maxThreadNum;

    @Value("${default.threadPool.queueSize}")
    private int queueSize;

    private String threadPrefix = "default-worker";

    @Override
    public void execute(Runnable task) {
        threadPoolExecutor.execute(task);
    }

    @Override
    public List execute(List taskList, DefaultThreadCallback callback, ThreadPoolExecutor threadPoolExecutor, DefaultThreadTaskCall threadTaskCall) throws Exception {
        if (threadPoolExecutor != null) {
            this.threadPoolExecutor = threadPoolExecutor;
        }
        List<Future> futures = multiProcess(taskList, callback, threadTaskCall);
        if (CollectionUtils.isEmpty(futures)) {
            return null;
        }
        return parseList(futures);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return threadPoolExecutor.submit(task);
    }

    @Override
    public void afterPropertiesSet() {
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>(queueSize);
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true).setNameFormat(threadPrefix + " - %s").build();
        threadPoolExecutor = new ThreadPoolExecutor(coreThreadNum, maxThreadNum, 5L, TimeUnit.SECONDS, taskQueue, threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
        log.info("启动线程池服务,coreSize:[{}],maxSize:[{}]", coreThreadNum, maxThreadNum);
    }

    private List multiProcess(List taskList, DefaultThreadCallback callback, DefaultThreadTaskCall threadTaskCall) throws Exception {
        CountDownLatch countDown = new CountDownLatch(taskList.size());
        List<Future> futures = new ArrayList<>();
        for (Object object : taskList) {
            threadTaskCall.setObject(object);
            threadTaskCall.setDockingThreadCallback(callback);
            threadTaskCall.setCountDown(countDown);
            futures.add(threadPoolExecutor.submit(threadTaskCall));
        }
        countDown.await();
        return futures;
    }

    private List parseList(List<Future> futures) throws Exception {
        List dataList = new ArrayList();
        for (Future future : futures) {
            dataList.add(future.get());
        }
        return dataList;
    }

    @Override
    public void setCoreThreadNum(int coreThreadNum) {
        this.coreThreadNum = coreThreadNum;
    }

    @Override
    public void setMaxThreadNum(int maxThreadNum) {
        this.maxThreadNum = maxThreadNum;
    }

    @Override
    public void setThreadPrefix(String threadPrefix) {
        this.threadPrefix = threadPrefix;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return this.threadPoolExecutor;
    }
}
