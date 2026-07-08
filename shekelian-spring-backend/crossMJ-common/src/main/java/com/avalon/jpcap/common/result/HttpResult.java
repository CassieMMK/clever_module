package com.avalon.jpcap.common.result;

/**
 * @project: crossMJ
 * @description:
 * @author: DingHaoLun
 * @create: 2022-09-28 17:20
 **/

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Data
public class HttpResult<T> implements Serializable {
    /**
     * 成功的码
     */
    private static final String CODE_SUCCESS = "0";
    /**
     * "结果：true-成功，false-失败"
     */
    @ApiModelProperty("接口结果：成功，失败")
    private Boolean success;
    /**
     * "错误码：成功-0，其余错误码定义"
     */
    @ApiModelProperty("错误码：成功-0，其余错误码定义")
    private String code;
    /**
     * 错误消息
     */
    @ApiModelProperty("错误消息")
    private String msg;
    /**
     * 数据
     */
    @ApiModelProperty("数据")
    private T data;
    /**
     * 跟踪号
     */
    @ApiModelProperty("跟踪号")
    private String traceId;
    /**
     * 扩展数据
     */
    @ApiModelProperty("扩展数据")
    private Object ext;

    /**
     * 成功返回
     * @param data
     * @param <T>
     * @return
     */
    public static <T> HttpResult<T> success(T data) {
        HttpResult<T> result = new HttpResult<T>();
        result.setSuccess(true);
        result.setCode(CODE_SUCCESS);
        result.setTraceId(MDCHelper.getTraceId());
        result.setData(data);
        return result;
    }

    /**
     * 成功返回
     *
     * @param data    数据
     * @param traceId 跟踪号
     * @param <T>     类型
     * @return 结果
     */
    public static <T> HttpResult<T> success(T data, String traceId) {
        HttpResult<T> result = new HttpResult<T>();
        result.setSuccess(true);
        result.setCode(CODE_SUCCESS);
        result.setTraceId(StringUtils.defaultString(traceId, MDCHelper.getTraceId()));
        result.setData(data);
        return result;
    }

    /**
     * 失败返回
     *
     * @param errorCode    错误
     * @param errorMessage 消息
     * @param <T>          类型
     * @return 结果
     */
    public static <T> HttpResult<T> failure(String errorCode,
                                        String errorMessage) {
        return failure(errorCode, errorMessage, null);
    }

    /**
     * 失败返回
     *
     * @param errorCode    错误码
     * @param errorMessage 错误消息
     * @param traceId      跟踪号
     * @param <T>          类型
     * @return 结果
     */
    public static <T> HttpResult<T> failure(String errorCode, String errorMessage,
                                        String traceId) {
        HttpResult<T> result = new HttpResult<T>();
        result.setSuccess(false);
        result.setTraceId(StringUtils.defaultString(traceId, MDCHelper.getTraceId()));
        result.setCode(errorCode);
        result.setMsg(errorMessage);
        return result;
    }

    /**
     * 失败返回
     *
     * @param errorCode    错误码
     * @param errorMessage 错误消息
     * @param traceId      traceId
     * @param data         数据
     * @param <T>          类型
     * @return 结果
     */
    public static <T> HttpResult<T> failure(String errorCode, String errorMessage,
                                        String traceId, T data) {
        HttpResult<T> result = failure(errorCode, errorMessage, traceId);
        result.setData(data);
        return result;
    }


}
