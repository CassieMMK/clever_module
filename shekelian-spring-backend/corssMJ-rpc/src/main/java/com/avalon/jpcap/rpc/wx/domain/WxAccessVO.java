package com.avalon.jpcap.rpc.wx.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信AccessToken http返回体
 * @author DingHaoLun
 * @since 2022-11-08 14:15
 **/
@Data
public class WxAccessVO implements Serializable {

    /**
     * ac_token
     */
    private String access_token;

    /**
     * unionid (用同一主体id打通微信生态下的各个应用使用的统一身份标识。)
     */
    private String unionid;

    /**
     * 仅仅是当前这个应用（小程序 or 网页 or 公众号...)下的主体id身份标识
     */
    private String openid;

    /**
     * expires_in 过期时间
     */
    private Integer expires_in;

    private String refresh_token;

    private String scope;

    /**
     * 错误码
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;
}