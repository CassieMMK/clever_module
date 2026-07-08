package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-04-24 21:19
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "MJ variant图片提示词")
public class VariantImgVO extends BaseControllerDomain {

    @ApiModelProperty(value = "生成记录的主键id", required = true)
    private Long imgRecordId;

    @ApiModelProperty(value = "要二次生成的图片的id号", required = true)
    private Integer imageIndex;

    @ApiModelProperty(value = "data体", required = true)
    private MjRespVO data;
}