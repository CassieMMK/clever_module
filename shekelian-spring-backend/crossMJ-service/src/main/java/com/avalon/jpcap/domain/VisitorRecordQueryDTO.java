package com.avalon.jpcap.domain;

import lombok.Data;

import java.util.Date;

/**
 * @author DingHaoLun
 * @since 2022-12-15 15:54
 **/
@Data
public class VisitorRecordQueryDTO {

    private String province;

    private Date visitDayStart;

    private Date visitDayEnd;
}