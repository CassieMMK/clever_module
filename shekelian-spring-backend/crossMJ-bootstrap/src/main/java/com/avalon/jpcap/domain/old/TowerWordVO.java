package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;

/**
 * 通关塔顶留言
 *
 * @author DingHaoLun
 * @since 2023-03-22 11:11
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "塔顶刻字或塔内留言")
public class TowerWordVO extends BaseControllerDomain {

    @ApiModelProperty("留言id，每个留言都有自己的独立id；写入新留言无需传，查询时必返回")
    private Long wordId;

    @ApiModelProperty(value = "塔留言点(关卡)，塔顶刻字和塔内留言点是一样的数据结构，都是固定某些关卡有一个留言点", required = true)
    private Integer wordPosition;

    @ApiModelProperty(value = "刻字或留言，不超过20字符", required = true, allowableValues = "range[1-20]")
    @Size(min = 1,max = 20, message = "留言不能超过20个字")
    private String word;
}