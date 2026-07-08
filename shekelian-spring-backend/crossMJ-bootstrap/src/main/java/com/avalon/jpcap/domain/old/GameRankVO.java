package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.PageVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-02-07 16:25
 **/
@Data
@ApiModel(description = "排行榜")
public class GameRankVO extends PageVO {

    /**
     * 此玩家当前全国排名
     */
    @ApiModelProperty(value = "全国排名", required = true)
    private Integer nowChinaRank;

    /**
     * 当前全国共有多少人登顶
     */
    @ApiModelProperty(value = "全国共有多少人登顶", required = true)
    private Integer nowCompleteManNum;

    /**
     * 获取全国排名x->y的玩家
     */
    @ApiModelProperty(value = "全国排名x到y的玩家名单（已通过分页请求对应换算）", required = true)
    private List<HighestRecordVO> rankBossList;
}