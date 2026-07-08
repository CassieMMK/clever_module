package com.avalon.jpcap.common.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-03-09 21:39
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class PageQueryDTO extends BaseDtoDomain{

    /**
     * 当前页码
     */
    private Integer currentPage;

    /**
     * 页码大小
     */
    private Integer pageSize;
}