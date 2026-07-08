package com.avalon.jpcap.repository.po;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 分页基类
 */
@Data
@Accessors(chain = true)
public class PageQueryPO implements Serializable{

    /**
     * 当前页数
     */
    private int currentPage;

    /**
     * 分页大小
     */
    private int pageSize;
}
