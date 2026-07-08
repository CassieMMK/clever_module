package com.avalon.jpcap.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 第三方登录平台的枚举
 *
 * @author DingHaoLun
 * @since 2022-11-09 11:36
 **/
@Getter
@AllArgsConstructor
public enum LoginPlatformSourceEnum {

    WX(1, "微信"),
    ;

    private Integer code;
    private String msg;
}