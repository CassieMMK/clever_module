package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author DingHaoLun
 * @since 2023-03-25 11:12
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "塔内或塔顶留言或刻字查询")
public class TowerWordQueryVO extends BaseControllerDomain {

    /**
     * 获取数量(不超过50）
     */
    @Size(min = 1, max = 50, message = "查询数量为1-50条")
    @NotNull(message = "查询数量不能为空")
    @ApiModelProperty(value = "获取数量", required = true, position = 0)
    private Integer num;

    /**
     * 关卡
     */
    @NotNull(message = "获取留言点位置不能为空")
    @ApiModelProperty(value = "获取留言点关卡", required = true, position = 1)
    private Integer wordPosition;
}