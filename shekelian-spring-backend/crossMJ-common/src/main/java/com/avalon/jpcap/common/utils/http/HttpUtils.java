package com.avalon.jpcap.common.utils.http;

import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static com.avalon.jpcap.common.enums.ResultCodeEnum.PARAM_ERROR;
import static com.avalon.jpcap.common.enums.ResultCodeEnum.RPC_ERROR;

/**
 * @project: crossMJ
 * @description: http工具
 * @author: DingHaoLun
 * @create: 2022-09-26 16:09
 **/
@Slf4j
public class HttpUtils {

    public static void checkUrl(String url){
        if(Objects.isNull(url) || StringUtils.isBlank(url)){
            throw new BusinessException(PARAM_ERROR.getCode(),"url地址为空");
        }
        if(!url.startsWith("http")){
            throw new BusinessException(PARAM_ERROR.getCode(),"url地址错误");
        }
    }

    /**
     * get方法执行http请求
     * @param
     */
    public static String get(String url) throws URISyntaxException, IOException {
        //1.获得一个httpclient对象
        CloseableHttpClient httpclient = HttpClients.createDefault();
        //2.生成一个get请求
        HttpGet httpGet = new HttpGet(url);
        //3.执行get请求并返回结果
        CloseableHttpResponse response = null;

        try {
            // 执行http get请求
            response = httpclient.execute(httpGet);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                return content;
            } else {
                throw new BusinessException(RPC_ERROR.getCode(),"获取微信登录信息失败");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(RPC_ERROR.getCode(),"获取微信登录信息失败");
        } finally {
            if (response != null) {
                response.close();
            }
            httpclient.close();
        }
    }

    /**
     * post方法执行http请求
     * 注意：HttpClient客户端这里是创建新的，如果每个请求都创建一个，client就得不到复用。每次创建新的内存回收需要频繁执行。java靠垃圾回收，无法手动删除对象
     * */
    public static <T> T doPost(String host, String path, String jsonParams, Integer timeout, Class<T> responseType) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault(); //每次请求创建了一个新的http客户端，消耗比较大
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout)
                .build();
        HttpPost httpPost = new HttpPost(host + path);
        StringEntity stringentity = new StringEntity(jsonParams, ContentType.APPLICATION_JSON);
        httpPost.setEntity(stringentity);
        httpPost.setConfig(requestConfig);
        httpPost.addHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());

        //3.执行get请求并返回结果
        CloseableHttpResponse response = null;

        try {
            // 执行http get请求
            response = httpClient.execute(httpPost);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                return parseToApiResponse(response, responseType);
            } else {
                throw new BusinessException(RPC_ERROR.getCode(),"post请求解析失败");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new BusinessException(RPC_ERROR.getCode(),"post请求解析失败");
        } finally {
            if (response != null) {
                response.close();
            }
            httpClient.close();
        }
    }

    public static <T> T parseToApiResponse(CloseableHttpResponse httpResponse, Class<T> responseType) throws IOException {
        Map<String, List<String>> headers = new HashMap<>();
        String message = httpResponse.getStatusLine().getReasonPhrase();

        //获取header
        Header[] var4 = httpResponse.getAllHeaders();
        int var5 = var4.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            Header header = var4[var6];
            List<String> values = (List)headers.get(header.getName());
            if (values == null) {
                values = new ArrayList();
            }

            ((List)values).add(header.getValue());
            log.info("header.getValue() : " + header.getValue());
            headers.put(header.getName().toLowerCase(), values);
        }


        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            Header contentType = entity.getContentType();
            if (contentType != null) {
                log.debug("content type : " + contentType.getValue());
            } else {
                log.debug("application/text; charset=utf-8");
            }

            String resp = EntityUtils.toString(entity, "UTF-8");
            T data = JsonUtils.fromJson(resp, responseType);
            return data;
        } else {
            String contentTypeStr = headers.containsKey("content-type") && ((List)headers.get("content-type")).size() > 0 ? (String)((List)headers.get("content-type")).get(0) : null;
            if (null == contentTypeStr) {
                contentTypeStr = "application/text; charset=utf-8";
            }

            log.info("contentTypeStr:",contentTypeStr);
        }
        return null;
    }
}