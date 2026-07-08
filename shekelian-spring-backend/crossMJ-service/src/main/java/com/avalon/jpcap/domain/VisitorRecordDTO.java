package com.avalon.jpcap.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author DingHaoLun
 * @since 2022-12-15 15:53
 **/
@Data
public class VisitorRecordDTO {

    /**
     * 用户请求的来自省份
     */
    private String province;

    /**
     * 当日此省份的用户请求次数
     */
    private Integer provinceVisitCountEachDay;

    //sql.Date只包含 年月日
    /**
     * 日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date visitDay;
}