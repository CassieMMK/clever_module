package com.avalon.jpcap.common.utils.http;

import com.avalon.jpcap.common.config.HttpClientPoolConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.util.Assert;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
/**
 * http连接池
 *
 * @author DingHaoLun
 * @since 2023-04-14 17:28
 **/
@Slf4j
public class HttpClientPool {

    /**连接池配置*/
    private HttpClientPoolConfig httpClientConfig;

    /**apache 连接池管理器*/
    private PoolingHttpClientConnectionManager poolingHttpClientConnectionManager;

    /**httpClient客户端，可以被设置为内部使用连接池的方式，客户端只需要一个就行了*/
    private CloseableHttpClient closeableHttpClient;

    /**监控连接池状态的线程*/
    private HttpClientPoolMonitorThread httpClientPoolMonitorThread;



    /**
     * 初始化实例
     * @param httpClientConfig
     */
    public HttpClientPool(HttpClientPoolConfig httpClientConfig) {
        Assert.notNull(httpClientConfig, "httpClientConfig argument must not be null");
        this.httpClientConfig = httpClientConfig;
        this.init(httpClientConfig);
    }


    /**
     * 监控连接池链接状态的线程
     * @date 2019/12/18 9:09
     **/
    private final static class HttpClientPoolMonitorThread extends Thread {
        private final HttpClientConnectionManager httpClientConnectionManager;
        private final HttpClientPoolConfig httpConfig;
        private volatile boolean shutdown;
        private Object lock = new Object();

        public HttpClientPoolMonitorThread(HttpClientConnectionManager httpClientConnectionManager, HttpClientPoolConfig httpConfig) {
            this.httpClientConnectionManager = httpClientConnectionManager;
            this.httpConfig = httpConfig;
            this.shutdown = false;
        }

        @Override
        public void run() {
            try {
                while (!this.shutdown) {
                    synchronized (lock) {
                        lock.wait(this.httpConfig.getMonitorInterval());
                        // 关闭无效的连接
                        this.httpClientConnectionManager.closeExpiredConnections();
                        // 关闭空闲时间超过IDLE_ALIVE_MS的连接
                        this.httpClientConnectionManager.closeIdleConnections(this.httpConfig.getIdleTimeOut(), TimeUnit.MILLISECONDS);
                    }
                }
            } catch (InterruptedException e) {
                log.warn("", e);
            }
        }

        public void shutdown() {
            this.shutdown = true;
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }


    public CloseableHttpClient getCloseableHttpClient() {
        return closeableHttpClient;
    }

    /**
     * 关闭连接池及管理器线程
     * @throws IOException
     */
    public void close() throws IOException {
        this.closeableHttpClient.close();
        this.httpClientPoolMonitorThread.shutdown();
        this.poolingHttpClientConnectionManager.close();
    }







    private void init(HttpClientPoolConfig httpClientConfig) {
        this.poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        this.poolingHttpClientConnectionManager.setDefaultMaxPerRoute(httpClientConfig.getDefaultMaxPerRoute());
        this.poolingHttpClientConnectionManager.setMaxTotal(httpClientConfig.getMaxTotal());
        this.createHttpClient();
    }

    /**
     * 设置httpClient为使用连接池的方式，只需要一个客户端，这个客户端是线程安全的，每次请求都会从连接池中挑一个空闲的http连接复用
     */
    private void createHttpClient() {
        HttpRequestRetryHandler httpRequestRetryHandler = (exception, executionCount, context) -> {
            if (executionCount >= this.httpClientConfig.getRetryCnt() // 如果已经重试了3次，就放弃
                    || exception instanceof SSLHandshakeException // 不要重试SSL握手异常
                    || exception instanceof InterruptedIOException // 超时
                    || exception instanceof UnknownHostException // 目标服务器不可达
                    || exception instanceof ConnectTimeoutException // 连接被拒绝
                    || exception instanceof SSLException // SSL握手异常
            ) {
                return false;
            } else if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
                return true;
            }

            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            // 如果请求是幂等的，就再次尝试
            if (!(request instanceof HttpEntityEnclosingRequest)) {
                return true;
            }
            return false;
        };

        //设置httpCLient为使用poolingHttpClientConnectionManager连接池的客户端，只需要初始化一次。以后closeableHttpClient在请求连接的时候，对每次请求都会自动找一个寻空闲的http连接来复用
        this.closeableHttpClient = HttpClients.custom().setConnectionManager(this.poolingHttpClientConnectionManager).setRetryHandler(httpRequestRetryHandler).build();
        this.httpClientPoolMonitorThread = new HttpClientPoolMonitorThread(this.poolingHttpClientConnectionManager, this.httpClientConfig);
        this.httpClientPoolMonitorThread.start();
    }

}

