package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author DingHaoLun
 * @since 2023-04-20 20:02
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "Mj排队及处理结果")
public class MjProcessRespVO extends BaseControllerDomain {

    @ApiModelProperty(value = "stream mq队列里面的id号")
    private String mqId;

    @ApiModelProperty(value = "uv操作的父图片信息", required = true)
    private FatherImgRecordVO metaData;

    @ApiModelProperty(value = "生成记录的主键id", required = true)
    private Long imgRecordId;;

    @ApiModelProperty(value = "-1为上一个已成功当前没有进行中的。 0-为正在处理中， 1及以上为前面排队多少人", required = true)
    private Integer rank;

    @ApiModelProperty(value = "大约等待多少分钟，当rank>=1时有用")
    private Integer waitTime;

    /**
     * 请求类型 @see com.avalon.jpcap.common.enums.MjDiscordSendTypeEnum
     */
    @ApiModelProperty(value = "任务是什么 1-生成  2-变换大小  3-变形")
    private Integer type;

    @ApiModelProperty(value = "请求时的prompt")
    private String prompt;

    @ApiModelProperty(value = "generate 、scale 和 virant 图片的统一出参")
    private MjRespVO data;
}