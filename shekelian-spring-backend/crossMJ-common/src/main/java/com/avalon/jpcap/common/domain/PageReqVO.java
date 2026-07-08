package com.avalon.jpcap.common.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author DingHaoLun
 * @since 2023-03-09 21:19
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PageReqVO extends BaseControllerDomain{

    /**
     * 当前页码
     */
    @NotNull(message = "currentPage can not be null")
    @ApiModelProperty(value = "当前页码", position = 0, required = true)
    private Integer currentPage;

    /**
     * 页码大小
     */
    @NotNull(message = "pageSize can not be null")
    @ApiModelProperty(value = "分页大小", position = 1, required = true)
    private Integer pageSize;
}