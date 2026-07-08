package com.avalon.jpcap.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @Author huangwenjun
 * @Date 2022/4/8
 * @Description
 **/
@AllArgsConstructor
@Getter
public enum ResultCodeEnum {

    SUCCESS("0", "成功"),
    FAIL("2", "失败"),
    SYSTEM_ERROR("3", "系统异常"),
    PARAM_ERROR("4", "参数错误"),
    RPC_ERROR("5", "外部接口异常"),
    FORBIDDEN("9998","没有访问权限"),
    UNAUTHORIZED("9999","未登录，无权限"),

    ;

    private String code;
    private String msg;
}
