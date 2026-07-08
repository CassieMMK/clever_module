package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author DingHaoLun
 * @since 2023-04-10 22:14
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "MJ生成提示词")
public class PromptVO extends BaseControllerDomain {
    /**
     * 提示词
     */
    @ApiModelProperty(value = "提示词必须是英文并用\"逗号\"隔开，前端后端共同校验", position = 0, required = true)
    @NotNull(message = "提示词不能为空")
    @NotBlank(message = "提示词不能为空")
    private String prompt;
}