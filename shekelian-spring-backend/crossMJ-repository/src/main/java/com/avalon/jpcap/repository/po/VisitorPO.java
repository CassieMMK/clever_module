package com.avalon.jpcap.repository.po;

import lombok.Data;

/**
 * @author DingHaoLun
 * @since 2022-12-15 15:07
 * @TableName(value = "visitor_record")
 */
@Data
public class VisitorPO extends BasePO {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 用户ip所属省份（或省级市）
     */
    private String province;

    /**
     * 当日某省份访问次数统计
     */
    private Integer provinceVisitCountEachDay;

    /**
     * 当日访问日期 yyyy-MM-dd
     */
    private String visitDay;
}