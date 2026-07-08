package com.avalon.jpcap.common.exceptions;

/**
 * @project: crossMJ
 * @description:
 * @author: DingHaoLun
 * @create: 2022-09-26 16:12
 **/
public class BaseException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 构造函数
     *
     * @param message   消息
     * @param errorCode 错误码
     */
    public BaseException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    /**
     * 构造函数
     *
     * @param message   错误消息
     * @param cause     原因
     * @param errorCode 错误码
     */
    public BaseException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode.getCode(), message, cause);
    }

    /**
     * 公共构造函数
     *
     * @param errorCode 错误码
     * @param message   消息
     */
    public BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 公共工造函数
     *
     * @param errorCode 错误码
     * @param message   错误消息
     * @param cause     原因
     */
    public BaseException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
