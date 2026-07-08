package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 会员充值
 *
 * @author DingHaoLun
 * @since 2023-05-11 22:12
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "用户会员充值（暂时没想好怎么设计和微信支付回调）")
public class MemberFundVO extends BaseControllerDomain {


}