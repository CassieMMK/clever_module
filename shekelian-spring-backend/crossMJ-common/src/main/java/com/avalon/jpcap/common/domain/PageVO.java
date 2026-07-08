package com.avalon.jpcap.common.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-03-09 21:13
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PageVO<T> extends BaseControllerDomain {

    /**
     * 当前页码
     */
    @ApiModelProperty(value = "当前页码", position = 0)
    private Integer currentPage;

    /**
     * 页码大小
     */
    @ApiModelProperty(value = "页码大小", position = 1)
    private Integer pageSize;

    /**
     * 总数
     */
    @ApiModelProperty(value = "总量", position = 2)
    private Integer totalCount;

    /**
     * 数据
     */
    @ApiModelProperty(value = "此页数据", position = 3)
    private List<T> data;
}