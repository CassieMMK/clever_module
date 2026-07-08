package com.avalon.jpcap.rpc.wx.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author DingHaoLun
 * @since 2023-07-03 15:00
 **/
@Data
@Accessors(chain = true)
public class WxPayCallBackVO implements Serializable {

    /**
     * 通知id
     */
    private String id;

    /**
     * 通知创建时间
     */
    private String create_time;

    /**
     * 通知类型
     */
    private String event_type;

    /**
     * 通知数据类型
     */
    private String resource_type;

    /**
     * 核心内容
     */
    private WxPayCallBackResourceVO resource;
}