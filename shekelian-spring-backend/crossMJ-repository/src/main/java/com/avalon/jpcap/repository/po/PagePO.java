package com.avalon.jpcap.repository.po;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @author DingHaoLun
 * @since 2022-12-17 20:40
 **/
@Data
@Accessors(chain = true)
public class PagePO<T> implements Serializable {

    /**
     * 当前页面
     */
    private Integer currentPage;

    /**
     * 页面尺寸
     */
    private Integer pageSize;

    /**
     * 数据总数
     */
    private Integer totalCount;

    /**
     * 数据
     */
    List<T> data;
}