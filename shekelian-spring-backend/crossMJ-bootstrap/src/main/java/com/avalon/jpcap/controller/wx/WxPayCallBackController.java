package com.avalon.jpcap.controller.wx;

/**
 * @author DingHaoLun
 * @since 2023-07-03 14:58
 **/

import com.avalon.jpcap.rpc.wx.domain.WxPayCallBackVO;
import com.avalon.jpcap.service.OrderService;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 微信回调支付结果所需的controoler
 */
@RestController
@RequestMapping("/wx")
public class WxPayCallBackController {

    @Resource
    private OrderService orderService;

    @PostMapping("/callback/wxPay")
    @ApiOperation("微信支付结果回调接口")
    public void getWxPayCallBack(@RequestBody WxPayCallBackVO reqVO){
        //TODO：查询订单号,将支付成功的结果修改进订单
//        orderService.queryOrder()

        //TODO：判断支付后，将用户的权益
    }
}