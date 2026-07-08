package com.avalon.jpcap.service;

import com.avalon.jpcap.rpc.wx.service.WxPayRpc;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 结算服务
 *
 * @author DingHaoLun
 * @since 2023-07-04 18:50
 **/
@Service
public class PayService {

    @Resource
    private WxPayRpc wxPayRpc;
}