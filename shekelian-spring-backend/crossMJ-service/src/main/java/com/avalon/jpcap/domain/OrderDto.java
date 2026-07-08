package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseDtoDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 订单
 *
 * @author DingHaoLun
 * @since 2023-07-17 15:49
 **/
@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderDto extends BaseDtoDomain {
    /**
     * 订单号主键id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 订单金额（单位为分）
     */
    private Integer amount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 支付时间
     */
    private Date payTime;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 支付渠道
     */
    private Integer channel;
}