package com.avalon.jpcap.rpc.wx.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author DingHaoLun
 * @since 2023-07-03 10:52
 **/
@Data
@Accessors(chain = true)
public class WxPayReqVO implements Serializable {

    /**
     * 用户请求终端ip
     */
    private String ip;

    /**
     * 请求类型
     * ios、android、web
     */
    private String type;

    /**
     * 系统内订单号
     */
    private String orderId;

    /**
     * 订单金额(金额为分）
     */
    private Integer amount;
}