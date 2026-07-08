package com.avalon.jpcap.skl.DTO;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "省课题机关排名统计信息DTO")
public class ProvinceTopicInfoDTO {
    @ApiModelProperty("机关ID")
    private Integer organizationId;

    @ApiModelProperty("机关姓名")
    private String organizationName;

    @ApiModelProperty("课题数量")
    private Integer topicCount;
}
