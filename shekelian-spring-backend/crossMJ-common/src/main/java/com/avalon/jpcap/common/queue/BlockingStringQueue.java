package com.avalon.jpcap.common.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 每个实现DefaultAbstractBlockingQueue的子类都必须初始化队列的大小
 * @author DingHaoLun
 * @since 2022-12-15 13:49
 **/

@Slf4j
@Component
public class BlockingStringQueue extends DefaultAbstractBlockingQueue<String> {

    @Value("${blockingStringQueue.capacity:2000}")
    private Integer capacity;

    @Value("${blockingStringQueue.numElements:100}")
    private Integer numElements;

    @Value("${blockingStringQueue.timeOutMins:1}")
    private Integer timeOutMins;

    @Override
    public void setParam() {
        super.setCapacity(capacity);
        super.setNumElements(numElements);
        super.setTimeOutMins(timeOutMins);
    }
}