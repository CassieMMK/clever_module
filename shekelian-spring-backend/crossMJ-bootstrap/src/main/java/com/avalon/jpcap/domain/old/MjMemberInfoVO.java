package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * Mj会员信息
 *
 * @author DingHaoLun
 * @since 2023-05-11 21:54
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "用户会员信息。 商业模式有：1、是积分通用，vip仅仅是包月优先排队   2、vip无包月有积分（优先排队积分），普通会员也有积分（正常排队积分）， expireTime不用")
public class MjMemberInfoVO extends BaseControllerDomain {

    @ApiModelProperty(value = "是否是此等级的会员", position = 0, required = true)
    @NotBlank(message = "是否是此等级的会员")
    private Boolean isMember;

    @ApiModelProperty(value = "会员等级 1-普通会员  2-VIP （优先排队）", position = 1, required = true)
    @NotBlank(message = "会员等级")
    private Integer memberLevel;

    @ApiModelProperty(value = "剩余积分", position = 2, required = true)
    @NotBlank(message = "剩余积分")
    private Integer credit;

    @ApiModelProperty(value = "包月到期时间", position = 3, required = true)
    @NotBlank(message = "包月到期时间")
    private Date expireTime;

}