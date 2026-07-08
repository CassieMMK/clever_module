package com.avalon.jpcap.common.config;

import lombok.Data;

/**
 * http连接池配置
 * @author DingHaoLun
 * @since 2023-04-14 17:22
 **/
@Data
public class HttpClientPoolConfig extends HttpRequestConfig {
    /**
     * 每个host的默认最大连接数
     * @date 2019/12/17 18:23
     **/
    private int defaultMaxPerRoute = 500;
    /**
     * 连接池里的最大连接数
     * @date 2019/12/17 18:33
     **/
    private int maxTotal = 500;
    /**
     * 链接空闲超时回收，单位毫秒
     * @date 2019/12/17 18:36
     **/
    private int idleTimeOut = 300000;
    /**
     * 链接重试次数
     * @date 2019/12/17 19:03
     **/
    private int retryCnt = 3;
    /**
     * 连接池监控间隔时长，单位毫秒
     * @date 2019/12/18 8:59
     **/
    private int monitorInterval = 2000;
}
