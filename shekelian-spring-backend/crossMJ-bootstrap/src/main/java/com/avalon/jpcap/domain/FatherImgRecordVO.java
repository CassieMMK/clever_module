package com.avalon.jpcap.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * uv  请求的来源父图片
 * @author DingHaoLun
 * @since 2023-04-26 21:44
 **/
@Data
public class FatherImgRecordVO {

    @ApiModelProperty(value = "u、v操作的父图片的imgRecordId", required = true)
    private Long imgRecordId;

    @ApiModelProperty(value = "u、v请求的时候使用的index", required = true)
    private Integer imageIndex;
}