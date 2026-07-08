package com.avalon.jpcap.common.utils.http;

import com.avalon.jpcap.common.config.HttpClientPoolConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;

/**
 * 采用了连接池方式的请求处理器，链接可复用，close方法是关闭连接池
 */
@Slf4j
public class PoolingHttpRequestHandler extends AbstractHttpRequestHandler {

    private HttpClientPoolConfig httpClientConfig;

    private HttpClientPool httpClientPool;

    /**
     * 默认初始化语句
     */
    public PoolingHttpRequestHandler() {
        this(new HttpClientPoolConfig());
    }

    /**
     * 初始化出连接池管理器manager，以及使用连接池的HttpClient客户端
     */
    public PoolingHttpRequestHandler(HttpClientPoolConfig httpClientConfig) {
        super(httpClientConfig);
        this.httpClientConfig = httpClientConfig;
        this.httpClientPool = new HttpClientPool(httpClientConfig);
    }

    @Override
    public void close() throws IOException {
        this.httpClientPool.close();
    }

    @Override
    protected CloseableHttpClient getCloseableHttpClient() {
        return this.httpClientPool.getCloseableHttpClient();
    }


}
