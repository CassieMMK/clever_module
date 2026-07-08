package com.avalon.jpcap.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TokenErrorEnum {

    TOKEN_PARSE_ERROR(1001, "登录状态解析失败"),
    ;

    private Integer code;
    private String msg;
}
