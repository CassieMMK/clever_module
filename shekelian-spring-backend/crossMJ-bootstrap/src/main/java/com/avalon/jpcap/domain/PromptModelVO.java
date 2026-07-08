package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author DingHaoLun
 * @since 2023-05-15 15:17
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PromptModelVO extends BaseControllerDomain {

    /**
     * 卡片id
     */
    @ApiModelProperty(value = "卡片id号，插入不传，更新必传", position = 0, required = false)
    private Long promptModelId;

    /**
     * 封装提示词
     */
    @ApiModelProperty(value = "封装的提示词", position = 1, required = true)
    @NotBlank(message = "卡片提示词不能为空")
    @NotNull(message = "卡片提示词不能为空")
    private String prompt;

    /**
     * 风格示例图片
     */
    @ApiModelProperty(value = "卡片示例图片，无图时前端用css图像兜底", position = 2, required = false)
    private String modelImgUrl;
}