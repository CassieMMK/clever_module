package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.PageReqVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 查询排名VO
 *
 * @author DingHaoLun
 * @since 2023-03-09 21:12
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel("游戏排名分页查询入参")
public class GameRankQueryVO extends PageReqVO {

    /**
     * 当前关卡
     */
    @ApiModelProperty(value = "当前关卡，查询当前关卡的全国排名时必传，查看用户的全国排名时不传", position = 0)
    private Integer nowTowerLevel;

}