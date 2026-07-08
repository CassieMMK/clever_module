package com.avalon.jpcap.common.exceptions;

/**
 * @project: crossMJ
 * @description:
 * @author: DingHaoLun
 * @create: 2022-09-26 16:13
 **/
public class BusinessException extends BaseException {
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public BusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}