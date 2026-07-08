package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author DingHaoLun
 * @since 2023-03-25 15:32
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "保存跳跃和坠落次数")
public class JumpAndFallVO extends BaseControllerDomain {

    /**
     * 跳跃次数
     */
    @ApiModelProperty(value = "跳跃次数", position = 0)
    private Integer jumpTimes;

    /**
     * 坠落次数
     */
    @ApiModelProperty(value = "坠落次数", position = 1)
    private Integer fallTimes;
}