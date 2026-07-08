package com.avalon.jpcap.repository.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 订单po
 *
 * @author DingHaoLun
 * @since 2023-07-04 17:45
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class OrderPO extends BasePO{

    /**
     * 主键id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 支付渠道
     */
    private Integer channel;

    /**
     * 完成状态
     */
    private Boolean finished;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 订单金额(单位：分）
     */
    private Integer amount;

    /**
     * 退款金额(单位：分）
     */
    private Integer refund;

    /**
     * 付款时间
     */
    private Date payTime;
}
