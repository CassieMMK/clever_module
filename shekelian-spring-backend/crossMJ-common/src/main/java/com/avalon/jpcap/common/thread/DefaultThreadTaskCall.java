package com.avalon.jpcap.common.thread;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class DefaultThreadTaskCall implements Callable {

    protected Object object;

    protected DefaultThreadCallback dockingThreadCallback;

    protected CountDownLatch countDown;

    public DefaultThreadTaskCall() {
    }

    DefaultThreadTaskCall(Object object, DefaultThreadCallback callback, CountDownLatch countDown) {
        this.object = object;
        this.countDown = countDown;
        this.dockingThreadCallback = callback;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public void setDockingThreadCallback(DefaultThreadCallback dockingThreadCallback) {
        this.dockingThreadCallback = dockingThreadCallback;
    }

    public void setCountDown(CountDownLatch countDown) {
        this.countDown = countDown;
    }

    @Override
    public Object call() throws Exception {
        try {
            return dockingThreadCallback.callback(object);
        } catch (Exception e) {
            log.error("并发处理业务失败,task={}", object, e);
            return null;
        } finally {
            countDown.countDown();
        }
    }
}