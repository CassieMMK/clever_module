package com.avalon.jpcap.skl.DTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "按年份统计课题DTO")
public class TopicCountByYearDTO {

    @ApiModelProperty("统计年份")
    private Integer year;
    @ApiModelProperty("每年的课题数量")
    private Integer topicCount;

}
