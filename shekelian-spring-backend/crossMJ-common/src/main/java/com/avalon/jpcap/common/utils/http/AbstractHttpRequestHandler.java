package com.avalon.jpcap.common.utils.http;

/**
 * http请求抽象类
 *
 * @author DingHaoLun
 * @since 2023-04-14 17:25
 **/

import com.avalon.jpcap.common.config.HttpRequestConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractHttpRequestHandler {

    private HttpRequestConfig httpRequestConfig;

    public AbstractHttpRequestHandler(HttpRequestConfig httpRequestConfig) {
        this.httpRequestConfig = httpRequestConfig;
    }

    /**
     * 关闭链接
     * @date 2019/12/18 11:01
     * @throws IOException
     **/
    protected abstract void close() throws IOException;

    /**
     * 获取已经绑定了连接池的httpclient客户端，这个客户端会自动从连接池中拿空闲的连接来复用
     * @return org.apache.http.impl.client.CloseableHttpClient
     **/
    protected abstract CloseableHttpClient getCloseableHttpClient();

    /**
     * post请求，请求参数是json格式
     * @throws IOException
     **/
    public String post(String url, @Nullable Map<String, String> headers, @Nullable String jsonParams) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (StringUtils.isNotBlank(jsonParams)) {
            httpPost.setEntity(new StringEntity(jsonParams, ContentType.APPLICATION_JSON));
        }
        return request(httpPost, headers);
    }
    /**
     * post请求，请求参数是K/V格式的form-data
     * @throws IOException
     **/
    public String post(String url, @Nullable Map<String, String> headers,@Nullable Map<String, Object> formData) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (formData != null) {
            List<NameValuePair> nvps = new ArrayList<>(formData.size());
            formData.entrySet().forEach(e -> nvps.add(new BasicNameValuePair(e.getKey(), String.valueOf(e.getValue()))));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        }
        return request(httpPost, headers);
    }
    /**
     * get请求，请求参数会自动拼接在url后面
     * @throws IOException
     **/
    public String get(String url, @Nullable Map<String, String> headers, @Nullable Map<String, Object> params) throws IOException {
        if (params != null) {
            StringBuilder sb = new StringBuilder();
            params.entrySet().forEach(e -> sb.append("&").append(e.getKey()).append("=").append(e.getValue()));
            String param = sb.toString().replaceFirst("&", "?");
            url = url + param;
        }
        return request(new HttpGet(url), headers);
    }

    /**httpClient将从连接池中获取一个http连接来复用，发送请求*/
    private String request(HttpRequestBase httpRequestBase, Map<String, String> headers) throws IOException {
        config(httpRequestBase, headers);
        CloseableHttpResponse response = null;
        try {
            //从线程池中获取一个http连接直接使用。
            response = this.getCloseableHttpClient().execute(httpRequestBase);
            HttpEntity entity = response.getEntity();
            //返回response的jsonString
            String result = EntityUtils.toString(entity);
            // 关闭流
            EntityUtils.consume(entity);
            return result;
        } catch (IOException e) {
            throw e;
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("", e);
                }
            }
        }
    }

    /**设置每一个http请求的超时时间等*/
    private void config(HttpRequestBase httpRequestBase, Map<String, String> headers) {
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(this.httpRequestConfig.getConnectionRequestTimeout())
                .setConnectTimeout(this.httpRequestConfig.getConnectTimeout())
                .setSocketTimeout(this.httpRequestConfig.getSocketTimeout())
                .build();
        if (headers != null) {
            headers.entrySet().forEach(e -> httpRequestBase.setHeader(e.getKey(), e.getValue()));
        }
        httpRequestBase.setConfig(config);
    }


}
