package com.avalon.jpcap.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MjMemberLevelEnum {

    NORMAL(1, "普通会员"),
    VIP(2, "VIP会员"),

    ;
    private Integer code;
    private String msg;
}
