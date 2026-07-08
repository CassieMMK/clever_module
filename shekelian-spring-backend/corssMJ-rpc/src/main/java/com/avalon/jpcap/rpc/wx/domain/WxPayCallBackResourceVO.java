package com.avalon.jpcap.rpc.wx.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author DingHaoLun
 * @since 2023-07-03 15:08
 **/
@Data
@Accessors(chain = true)
public class WxPayCallBackResourceVO implements Serializable {

    /**
     * 加密算法类型
     */
    private String algorithm;

    /**
     * 数据密文
     */
    private String ciphertext;

    /**
     * 附加数据
     */
    private String associated_data;

    /**
     * 原始类型
     */
    private String original_type;

    /**
     * 加密使用的随机串
     */
    private String nonce;
}