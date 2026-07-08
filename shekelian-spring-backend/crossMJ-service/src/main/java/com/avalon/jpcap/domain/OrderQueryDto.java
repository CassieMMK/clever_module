package com.avalon.jpcap.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author DingHaoLun
 * @since 2023-07-17 16:03
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderQueryDto {

    /**
     * 订单id（非必填）
     */
    private Long id;

    /**
     * 用户id（必填）
     */
    private Long userId;

    /**
     * 订单状态
     */
    private Integer status;
}