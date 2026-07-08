package com.avalon.jpcap.skl.DTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 复用
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "返回XXX总数DTO")
public class TotalNumsDTO {
    @ApiModelProperty("总数（行数）")
    private Integer count;
}
