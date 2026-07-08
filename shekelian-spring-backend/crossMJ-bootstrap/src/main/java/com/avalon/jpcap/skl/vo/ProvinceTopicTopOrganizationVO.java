package com.avalon.jpcap.skl.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "省课题发表数量机关排名统计展示VO")
public class ProvinceTopicTopOrganizationVO {
    @ApiModelProperty("机关排名")
    private Integer rank;

    @ApiModelProperty("机关名称")
    private String organizationName;

    @ApiModelProperty("课题数量")
    private Integer topicCountDisplay;
}
