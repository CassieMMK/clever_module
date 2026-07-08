package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author DingHaoLun
 * @since 2023-01-31 10:35
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "游戏道具")
public class ChangeGamePropsVO extends BaseControllerDomain {

    /**
     * 道具类型（枚举）
     */
    @NotNull(message = "道具类型不能为空")
    @ApiModelProperty(value = "道具类型枚举", position = 0, required = true)
    private Integer gamePropType;

    /**
     * 道具数量(增加或者减少)
     */
    @NotNull(message = "道具数量不能为空")
    @ApiModelProperty(value = "道具数量增减 +增 -减", position = 1, required = true)
    private Integer num;
}