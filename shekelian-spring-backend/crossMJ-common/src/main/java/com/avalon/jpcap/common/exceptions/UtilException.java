package com.avalon.jpcap.common.exceptions;

/**
 * @project: everyThingUtils
 * @description:
 * @author: DingHaoLun
 * @create: 2022-01-27 16:15
 **/
public class UtilException extends RuntimeException {
    public UtilException(String message) {
        super(message);
    }

    public UtilException(String message, Throwable cause) {
        super(message, cause);
    }
}