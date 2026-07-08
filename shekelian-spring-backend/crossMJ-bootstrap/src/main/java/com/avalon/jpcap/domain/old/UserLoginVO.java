package com.avalon.jpcap.domain.old;

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
 * @project: crossMJ
 * @description: 用户微信登录入参
 * @author: DingHaoLun
 * @create: 2022-09-30 15:46
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "微信授权登录")
public class UserLoginVO extends BaseControllerDomain {

    /**
     * wx code,微信获取openId用code
     */
    @NotNull(message = "微信登录code不能为空")
    @NotBlank(message = "微信登录code不能为空")
    @ApiModelProperty("wxcode")
    private String wxcode;
}