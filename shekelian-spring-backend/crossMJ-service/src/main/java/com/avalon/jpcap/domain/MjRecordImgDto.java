package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseDtoDomain;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * mj生成记录的dto
 *
 * @author DingHaoLun
 * @since 2023-04-20 20:21
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MjRecordImgDto extends BaseDtoDomain {

    /**以下是uv请求时的父图片的信息 start*/

    @ApiModelProperty(value = "u、v操作的父图片的imgRecordId", required = true)
    private Long fatherImgRecordId;

    @ApiModelProperty(value = "u、v请求的时候使用的index", required = true)
    private Integer childImgIndex;

    /**以下是uv请求时的父图片的信息 end*/

    /**
     * 用户生成图片记录主键id
     */
    private Long imgRecordId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 生成时间
     */
    private Date time;

    /**
     * 图片discord cdn
     */
    private String imgUrlOuter;

    /**
     * 图片国内cdn
     */
    private String imgUrl;

    /**
     * @see com.avalon.jpcap.common.enums.MjDiscordSendTypeEnum
     */
    private Integer type;

    /**
     * 是否被用户接收过
     */
    private Boolean used;
}