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
 * 最高记录高度保存入参
 *
 * @author DingHaoLun
 * @since 2023-01-30 21:19
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "复活点及存档点记录")
public class HighestRecordVO extends BaseControllerDomain {

    /**
     * 最高关卡
     */
    @ApiModelProperty(value = "最高复活点关卡",position = 0)
    private Integer highestTowerLevel;

    /**
     * 最高高度（米）
     */
    @ApiModelProperty(value = "最高高度", position = 1)
    private Integer maxHeight;

    /**
     * 当前关卡
     */
    @ApiModelProperty(value = "当前存档点关卡", position = 2)
    private Integer nowTowerLevel;

    /**
     * 是否通关
     */
    @ApiModelProperty(value = "是否通关 0-否 1-是", position = 4)
    private Integer finishGame;
}