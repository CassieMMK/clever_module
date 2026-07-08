package com.avalon.jpcap.common.thread;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 多线程处理
 */
public interface ThreadPoolService {

    void setCoreThreadNum(int threadNum);

    void setMaxThreadNum(int maxThreadNum);

    void execute(Runnable command);

    /**
     * 并发执行回调用函数
     * @param taskList
     * @param callback
     * @return
     */
    List execute(List taskList, DefaultThreadCallback callback, ThreadPoolExecutor threadPoolExecutor, DefaultThreadTaskCall threadTaskCall) throws Exception;

    void setThreadPrefix(String threadPrefix);
}
