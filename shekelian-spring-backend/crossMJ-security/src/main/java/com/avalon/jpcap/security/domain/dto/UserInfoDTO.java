package com.avalon.jpcap.security.domain.dto;

import com.avalon.jpcap.common.domain.BaseDtoDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author DingHaoLun
 * @since 2022-11-14 15:27
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class UserInfoDTO extends BaseDtoDomain {
    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户pin
     */
    private String pin;

    /**
     * 用户昵称
     */
    private String nickName;

    private String country;

    /**
     * 用户头像
     */
    private String headImgUrl;

    /**
     * 第三方平台
     */
    private Integer platform;

    /**
     * 第三方平台对应pin身份字段
     */
    private String platformPin;
}