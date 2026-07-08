package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.PageReqVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author DingHaoLun
 * @since 2023-05-15 15:31
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ApiModel(description = "提示词封装卡片查询-入参")
public class PromptModelQueryPageVO extends PageReqVO {

    /**
     * 按照被使用次数排序
     */
    @ApiModelProperty(value = "按照使用次数逆序排列(与点赞次数2选1)，若都不填则默认按照时间逆序", position = 1, required = false)
    private Boolean orderByUsedTimesDesc;

    /**
     * 按照被使用次数排序
     */
    @ApiModelProperty(value = "按照点赞次数逆序排列(与使用次数2选1)，若都不填则默认按照时间逆序", position = 2, required = false)
    private Boolean orderByLikeTimesDesc;
}