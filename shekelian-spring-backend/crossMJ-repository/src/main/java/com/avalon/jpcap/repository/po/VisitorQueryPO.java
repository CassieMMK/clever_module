package com.avalon.jpcap.repository.po;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author DingHaoLun
 * @since 2022-12-15 15:10
 **/
@Data
public class VisitorQueryPO implements Serializable {

    /**
     * 省份
     */
    private String province;

    /**
     * yyyy-MM-dd
     */
    private String visitDayStart;

    /**
     * yyyy-MM-dd
     */
    private String visitDayEnd;
}