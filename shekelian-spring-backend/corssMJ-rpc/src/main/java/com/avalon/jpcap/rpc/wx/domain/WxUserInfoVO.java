package com.avalon.jpcap.rpc.wx.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author DingHaoLun
 * @since 2022-11-08 15:20
 **/
@Data
@Accessors(chain = true)
public class WxUserInfoVO implements Serializable {

    /**
     * openId：用户在微信应用某个端（小程序 or 网页 or 公众号）的标识，每个端内唯一
     */
    private String openid;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 性别:
     * 1-男性，2-女性，0-未知
     */
    private Integer sex;

    /**
     * 省份
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 国家
     */
    private String country;

    /**
     * 用户头像
     */
    private String headimgurl;

    /**
     * 用户特权信息，json体
     */
    private Object[] privilege;

    /**
     * 只有在用户将公众号、网页端、小程序等都绑定到微信开放平台帐号后，才会出现该字段
     * （是用户在微信开发平台的唯一标识：即 端与端的合集下 唯一标识）
     */
    private String unionid;

    /**
     * 错误码
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;
}