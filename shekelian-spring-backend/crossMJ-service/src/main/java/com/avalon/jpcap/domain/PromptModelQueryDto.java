package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.PageQueryDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author DingHaoLun
 * @since 2023-05-15 14:12
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PromptModelQueryDto extends PageQueryDTO {

    /**
     * 卡片id
     */
    private Long id;

    /**
     * 创建者id
     */
    private Long userId;

    /**
     * 按照被使用过次数逆序排序
     */
    private Boolean orderByUsedTimesDesc;

    /**
     * 按照被点赞次数逆序排序
     */
    private Boolean orderByLikeTimesDesc;
}