package com.avalon.jpcap.common.config;

import lombok.Data;

/**
 * @author DingHaoLun
 * @since 2023-04-14 17:21
 **/
@Data
public class HttpRequestConfig {
    /**
     * 从连接池中获取一个httpClient的超时时间，单位毫秒
     **/
    private int connectionRequestTimeout = 1000;
    /**
     * 客户端和服务端建立连接的超时时间，单位毫秒
     **/
    private int connectTimeout = 2000;
    /**
     * 客户端从服务端读取数据的超时时间，单位毫秒
     **/
    private int socketTimeout = 300000;
}
