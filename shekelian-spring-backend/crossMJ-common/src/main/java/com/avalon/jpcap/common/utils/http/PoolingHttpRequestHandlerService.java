package com.avalon.jpcap.common.utils.http;

import com.avalon.jpcap.common.utils.JsonUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 连接池方式的http请求处理器的单例
 * 链接池配置采用了默认配置
 * @date 2019/12/18 10:02
 */
@Service
public class PoolingHttpRequestHandlerService {

    private PoolingHttpRequestHandler httpHandler;

    @PostConstruct
    private void initHttpClient() {
        this.httpHandler = new PoolingHttpRequestHandler();
    }

    /**
     * 从连接池中获取一个http连接复用，来执行json的post请求，并返回解析后的结果
     */
    public <T> T postJsonByHttpPool(String host, String path, String jsonParams, Integer timeout, Class<T> responseType) throws IOException {
        Map<String, String> header = new HashMap<>();
        header.put(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        String respString = httpHandler.post(host + path, header, jsonParams);
        //解析出参为dto对象,并返回
        return JsonUtils.fromJson(respString, responseType);
    }

}
