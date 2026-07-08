package com.avalon.jpcap.domain.old;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author DingHaoLun
 * @since 2023-05-07 23:46
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "用户信息")
public class UserLoginStateVO {

    @ApiModelProperty(value = "微信登录后重定向地址", position = 0, required = true)
    private String redirectURL;
}