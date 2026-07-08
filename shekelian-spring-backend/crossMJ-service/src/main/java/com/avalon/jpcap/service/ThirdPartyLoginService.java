package com.avalon.jpcap.service;

import com.avalon.jpcap.rpc.wx.domain.WxUserInfoVO;

/**
 * 换取第三方登录的pin
 * @author: DingHaoLun
 * @create: 2022-11-07 19:57
 **/
public interface ThirdPartyLoginService {

    /**
     * 【小程序专用】
     * 通过wxcode换取微信access_token,再通过accss_token去获取用户在微信存储的pin等信息
     */
    WxUserInfoVO getWxInfoByMP(String wxCode);

    /**
     * 【公众号、网页扫码登录、app用】
     * 通过wxcode换取微信access_token,再通过accss_token去获取用户在微信存储的pin等信息
     */
    WxUserInfoVO getWxInfo(String wxCode);

}
