package com.avalon.jpcap.repository.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author DingHaoLun
 * @since 2023-07-04 18:34
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class OrderQueryPO implements Serializable {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 完成状态
     */
    private Boolean finished;

    /**
     * 订单状态
     */
    private Integer status;
}