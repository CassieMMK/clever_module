package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseDtoDomain;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 国内midJourney请求
 *
 * @author DingHaoLun
 * @since 2023-04-13 21:55
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MjCrossDto extends BaseDtoDomain {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户提示词
     */
    private String prompt;

    /**
     * 请求类型 @see com.avalon.jpcap.common.enums.MjDiscordSendTypeEnum
     */
    private Integer type;

    /**
     * 请求体
     */
    private MjSendDto data;


    /**以下为传输stream时使用 start*/
    /**
     * vip
     */
    private Boolean vip;

    /**
     * 是否包月有效
     */
    private Boolean noExpire;

    /**
     * 是否使用积分且还有积分
     */
    private Boolean canUseCredit;
    /**以下为传输stream时使用 end*/


    @Data
    public static class MjSendDto{

        @ApiModelProperty(value = "u、v请求时使用的父图片的记录id", required = true)
        private Long fatherImgRecordId;

        @ApiModelProperty(value = "要操作的子图片的id号", required = true)
        private Integer imageIndex;

        @ApiModelProperty(value = "图片链接", required = true)
        private String url;

        @ApiModelProperty(value = "用户请求的提示词")
        private String prompt;

        @ApiModelProperty(value = "mj返回的提示词")
        private String response_prompt;

        @ApiModelProperty(value = "discord的消息message_id", required = true)
        private String message_id;

        @ApiModelProperty(value = "生成图片的4个变种的子图id", required = true)
        private List<String> image_IDs;

    }
}