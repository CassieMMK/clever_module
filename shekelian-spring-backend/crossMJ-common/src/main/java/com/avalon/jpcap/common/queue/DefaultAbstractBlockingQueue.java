package com.avalon.jpcap.common.queue;

import com.google.common.collect.Queues;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 创建阻塞队列工具类
 *
 * @author DingHaoLun
 * @since 2022-12-15 10:50
 **/
@Slf4j
@Data
public abstract class DefaultAbstractBlockingQueue<T>  {

    /**
     * 阻塞队列容量
     */
    private Integer capacity;

    /**
     * 批量消费时每多少个消费一次
     */
    private Integer numElements;

    /**
     * 批量消费超时时间（分钟）
     */
    private Integer timeOutMins;

    private BlockingQueue<T> blockingQueue;

    /**
     * 每个初始化的子类都必须实现,初始化 capacity、numElements、timeOutMins
     */
    public abstract void setParam();

    /**
     * bean初始化的时候创建一个队列
     */
    @PostConstruct
    public void init(){
        //创建队列,最大容量capacity
        this.setParam();
        BlockingQueue<T> blockingQueue = new ArrayBlockingQueue<T>(capacity,true);
        this.blockingQueue = blockingQueue;
        log.info("创建阻塞队列，容量为{}",capacity);
    }

    /**
     * 向队列中存放数据
     */public void saveQueueData(T t){
        //存放数据
        this.blockingQueue.offer(t);
    }

    /**
     * 从队列中单条消费数据
     */
    public T consumerBySingle() {
        while (true) {
            try {
                T t = blockingQueue.take();
                return t;
            } catch (Exception e) {
                log.error("缓存队列单条消费异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 从队列中批量消费数据
     */
    public List<T> consumerByBatch() {
        try {
            List<T> list = new ArrayList<>();
            //将阻塞队列里的数据消费到list中，每满numElements个批量消费一次，如果没满100个，则每隔1分钟也强制消费一次
            Queues.drain(blockingQueue, list, numElements, 1, TimeUnit.MINUTES);
            return list;
        } catch (Exception e) {
            log.error("缓存队列批量消费异常：{}", e.getMessage());
        }
        return null;
    }


}