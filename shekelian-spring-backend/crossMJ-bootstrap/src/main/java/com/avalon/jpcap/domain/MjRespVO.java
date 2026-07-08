package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 生成图片出参
 * 和mj的出参基本一致，@see com.avalon.jpcap.rpc.discord.domain.GenerateData
 * 如果是scale出参，只有url，其他的都值
 * @author DingHaoLun
 * @since 2023-04-20 20:05
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "generate和virant图片后 出参")
public class MjRespVO extends BaseControllerDomain {

    /**
     * 图片链接
     */
    @ApiModelProperty(value = "图片链接（scale返回只有此字段）")
    @NotNull(message = "url传入时不能为空")
    private String url;

    /**
     * 发送的提示词
     */
    @ApiModelProperty(value = "用户请求的提示词")
    @NotNull(message = "prompt传入时不能为空")
    private String prompt;

    /**
     * 返回的提示词
     */
    @ApiModelProperty(value = "mj返回的提示词")
    @NotNull(message = "response_prompt传入时不能为空")
    private String response_prompt;

    /**
     * discord消息id
     */
    @ApiModelProperty(value = "discord的消息message_id")
    @NotNull(message = "消息id传入时不能为空")
    private String message_id;

    /**
     * 生成图片的4个子图id
     */
    @ApiModelProperty(value = "生成图片的4个变种的子图id")
    @NotNull(message = "子图id传入时不能为空")
    private List<String> image_IDs;
}