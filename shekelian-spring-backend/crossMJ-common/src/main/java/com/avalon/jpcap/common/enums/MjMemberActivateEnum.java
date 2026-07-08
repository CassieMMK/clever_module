package com.avalon.jpcap.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author DingHaoLun
 * @since 2023-04-17 17:14
 **/
@Getter
@AllArgsConstructor
public enum MjMemberActivateEnum {

    INVALID(0, "无效"),
    VALID(1, "有效"),
    FREEZE(2,"被冻结")

    ;
    private Integer code;
    private String msg;
}