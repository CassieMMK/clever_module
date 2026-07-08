package com.avalon.jpcap.common.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @project: crossMJ
 * @description:
 * @author: DingHaoLun
 * @create: 2022-09-30 15:48
 **/
@Data
public class BaseControllerDomain implements Serializable {

    /**
     * 扩展数据
     */
    @ApiModelProperty(value = "未来预留扩展数据")
    private Object ext;
}