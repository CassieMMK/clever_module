package com.avalon.jpcap.service.impl;

import com.avalon.jpcap.rpc.wx.service.WxAccessRpc;
import com.avalon.jpcap.rpc.wx.domain.WxUserInfoVO;
import com.avalon.jpcap.service.ThirdPartyLoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 三方登录服务
 * @author DingHaoLun
 * @since 2022-11-07 20:15
 **/
@Service
@Slf4j
public class ThirdPartyLoginServiceImpl implements ThirdPartyLoginService {

    /**微信接口服务*/
    @Resource
    private WxAccessRpc wxAccessRpc;

    @Override
    public WxUserInfoVO getWxInfoByMP(String wxCode) {
        WxUserInfoVO wxUserInfoVO = wxAccessRpc.getWxUserByMP(wxCode);
        return wxUserInfoVO;
    }

    @Override
    public WxUserInfoVO getWxInfo(String wxCode) {
        WxUserInfoVO wxUserInfoVO = wxAccessRpc.getWxUser(wxCode);
        return wxUserInfoVO;
    }


}