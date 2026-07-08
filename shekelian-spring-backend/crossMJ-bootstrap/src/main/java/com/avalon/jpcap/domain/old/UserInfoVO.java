package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-03-23 20:37
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "用户信息")
public class UserInfoVO extends BaseControllerDomain {

    /**
     * 用户昵称
     */
    @ApiModelProperty(value = "用户昵称", position = 0, required = true)
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 用户头像
     */
    @ApiModelProperty(value = "用户头像", position = 1, required = true)
    @NotBlank(message = "头像不能为空")
    private String avatar;

    @ApiModelProperty(value = "用户会员信息", position = 2, required = true)
    @NotBlank(message = "用户不同等级的会员信息都返回，前端根据业务展示")
    List<MjMemberInfoVO> mjMemberInfoVOList;
}