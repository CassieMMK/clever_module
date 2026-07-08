package com.avalon.jpcap.skl.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class TotalNumsVO {
    @ApiModelProperty("总数")
    private Integer count;
}
