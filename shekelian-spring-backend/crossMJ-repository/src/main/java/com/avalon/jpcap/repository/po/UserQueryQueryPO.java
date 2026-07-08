package com.avalon.jpcap.repository.po;

import lombok.Data;

/**
 * @author huoqiang
 * @description
 * @date 2021/9/26
 */
@Data
public class UserQueryQueryPO extends PageQueryPO {

    /**
     * 用户id （主键）
     */
    private String userId;

    /**
     * 第三方平台
     */
    private Integer platform;

    /**
     * 第三方平台的pin
     */
    private String platformPin;
}
