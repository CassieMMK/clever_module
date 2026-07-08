package com.avalon.jpcap.repository.semaphore;

import java.util.concurrent.TimeUnit;

/**
 * 信号量阻塞服务
 *
 * @author DingHaoLun
 * @since 2023-04-13 21:43
 **/
public interface RedisSemaphoreService {

    /**
     * 初始化信号量
     * @param singleName 信号量的key
     * @param permits 信号量的数量
     * @return
     */
    void initSemaphore(String singleName, Integer permits);

    /**
     * 非公平方式抢夺信号量（阻塞式）
     * @return
     * @throws InterruptedException
     */
    String blockAcquire(String singleName, long leaseTime, TimeUnit unit) throws InterruptedException;

    /**
     * 非公平方式抢夺信号量（非阻塞式）
     * @param singleName
     * @return
     * @throws InterruptedException
     */
    String acquire(String singleName) throws InterruptedException;

    /**
     * 释放信号量
     * @return
     * @throws InterruptedException
     */
    Boolean release(String singleName, String releaseKey) throws InterruptedException;

    /**
     * 更新延长锁的持有时间
     */
    void updateReleaseTime(String singleName,String releaseKey, long leaseTime, TimeUnit unit);

    Integer getPermitAvalible(String singleName);
}