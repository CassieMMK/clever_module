package com.avalon.jpcap.common.thread;

/**
 * @author putengfei
 * @date 2021/9/27 下午2:38
 */
public interface DefaultThreadCallback<R,P> {
    R callback(P o);
}
