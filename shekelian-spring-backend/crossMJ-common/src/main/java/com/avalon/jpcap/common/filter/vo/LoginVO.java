package com.avalon.jpcap.common.filter.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * 登录对象
 */
@Data
@Accessors(chain = true)
public class LoginVO {
    /**
     * Header头
     */
    public static final String HEADER_PIN = "X-fastbe-pin";
    public static final String HEADER_VENDER_ID = "X-fastbe-venderId";
    public static final String HEADER_IDENTITY = "X-fastbe-identity";
    /**
     * 登录的PIN
     */
    private String pin;
    /**
     * 登录的商家ID
     */
    private String venderId;
    /**
     * 租户ID，和buId值一致
     */
    @Deprecated
    private String tenantId;
    /**
     * BU-ID
     */
    private String buId;
    /**
     * 场景Code
     */
    private String scenario;
    /**
     * 业务身份
     */
    private String identity;
    /**
     * 自定义扩展数据
     */
    private Map<String, Object> ext;

    /**第三方平台*/
    private Integer platform;

    /**第三方平台身份pin*/
    private String platformPin;

    /**
     * 用户id
     */
    private Long userId;
}
