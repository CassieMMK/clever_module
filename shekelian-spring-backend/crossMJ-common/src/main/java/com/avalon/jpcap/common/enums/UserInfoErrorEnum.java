package com.avalon.jpcap.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author DingHaoLun
 * @since 2022-10-31 21:15
 **/

@AllArgsConstructor
@Getter
public enum UserInfoErrorEnum {
    SAME_PLATEFORM_PIN("2", "此账户已经被注册过，请直接登录"),
    FAIL("2", "失败"),

    ;
    private String code;
    private String msg;
}