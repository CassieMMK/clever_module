package com.avalon.jpcap.rpc.wx.service;

import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.rpc.wx.domain.WxPayReqVO;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.h5.model.Amount;
import com.wechat.pay.java.service.payments.h5.model.PrepayRequest;
import com.wechat.pay.java.service.payments.h5.model.PrepayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 微信支付服务
 *
 * @author DingHaoLun
 * @since 2023-06-30 17:57
 **/
@Service
@Slf4j
public class WxPayRpc {


    /**
     * appId
     */
    @Value("wx.pay.appId")
    private String appId;
    /** 商户号 */
    @Value("wx.pay.merchantId")
    private String merchantId = "";
    /** 商户API私钥路径 */
    @Value("wx.pay.privateKeyPath")
    private String privateKeyPath = "";
    /** 商户证书序列号 */
    @Value("wx.pay.merchantSerialNumber")
    private String merchantSerialNumber = "";
    /** 商户APIV3密钥 */
    @Value("wx.pay.apiV3key")
    private String apiV3key = "";

    /**
     * 生成支付的二维码字符串
     */
    public String payH5Url(WxPayReqVO reqVO) {
        // 使用自动更新平台证书的RSA配置
        // 一个商户号只能初始化一个配置，否则会因为重复的下载任务报错
        Config config =
                new RSAAutoCertificateConfig.Builder()
                        .merchantId(merchantId)
                        .privateKeyFromPath(privateKeyPath)
                        .merchantSerialNumber(merchantSerialNumber)
                        .apiV3Key(apiV3key)
                        .build();
        // 构建service
        H5Service service = new H5Service.Builder().config(config).build();
        PrepayRequest request = new PrepayRequest();
        //基本调用参数
        request.setAppid("wxa9d9651ae******");
        request.setMchid(merchantId);
        request.setDescription("果赖赖MJ会员服务");
        request.setNotifyUrl("https://huahuaguolai.com/wx/callback/wxPay"); //TODO：配置化
        //业务参数
        Amount amount = new Amount();
        amount.setTotal(reqVO.getAmount());
        amount.setCurrency("CNY");
        request.setAmount(amount);//金额
        request.setOutTradeNo(reqVO.getOrderId());//系统内部订单号
        // 调用下单方法，得到应答
        log.info(this.getClass().getName() + ",param={}", JsonUtils.toJson(request));
        PrepayResponse response = service.prepay(request);
        log.info(this.getClass().getName() + ",result={}", JsonUtils.toJson(response));
        // 使用微信扫描 h5_url中间页对应的二维码，跳转到微信支付
        return response.getH5Url();
    }
}