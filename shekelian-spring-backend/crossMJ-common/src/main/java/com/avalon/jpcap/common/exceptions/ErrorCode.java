package com.avalon.jpcap.common.exceptions;

/**
 * @project: crossMJ
 * @description:
 * @author: DingHaoLun
 * @create: 2022-09-26 16:12
 **/
public interface ErrorCode {

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    String getCode();

    /**
     * 获取错误提示
     *
     * @return 错误提示
     */
    String getMessage();
}