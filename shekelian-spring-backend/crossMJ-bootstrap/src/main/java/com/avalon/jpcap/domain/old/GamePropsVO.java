package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * @author DingHaoLun
 * @since 2023-01-30 21:32
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "游戏道具")
public class GamePropsVO extends BaseControllerDomain {

    /**
     * 道具类型（枚举）
     */
    @NotNull(message = "道具不能为空")
    @ApiModelProperty(value = "道具类型枚举", position = 0, required = true)
    private Integer gamePropType;

    /**
     * 剩余道具数量
     */
    @NotNull(message = "数量不能为空")
    @ApiModelProperty(value = "道具剩余数量", position = 1, required = true)
    private Integer num;
}