package com.avalon.jpcap.repository.po;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @project: crossMJ
 * @description: 用户表映射
 * @author: DingHaoLun
 * @create: 2022-09-30 16:15
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
/**
 * TableName(value = "takevideo_user_info")
 */
public class UserInfoPO extends BasePO {

    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户pin
     */
    private String pin;

    /**
     * 第三方平台
     */
    private Integer platform;

    /**
     * 第三方平台对应pin身份字段
     */
    private String platformPin;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户头像
     */
    private String imgUrl;
}
