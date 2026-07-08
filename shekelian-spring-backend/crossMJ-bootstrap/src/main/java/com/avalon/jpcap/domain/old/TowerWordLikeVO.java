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
 * @since 2023-03-22 14:20
 **/
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description = "复活点及存档点记录")
public class TowerWordLikeVO extends BaseControllerDomain {

    /**
     * 留言id
     */
    @ApiModelProperty(value = "点赞的留言id", required = true)
    @NotNull(message = "点赞的留言id不能为空")
    private Long wordId;
}
