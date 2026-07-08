package com.avalon.jpcap.repository.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author DingHaoLun
 * @since 2023-05-15 13:50
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PromptModelQueryPO extends PageQueryPO{

    /**
     * 卡片id
     */
    private Long id;

    /**
     * 创建者id
     */
    private Long userId;

    /**
     * 按照被使用次数排序
     */
    private Boolean orderByUsedTimesDesc;

    /**
     * 按照被使用次数排序
     */
    private Boolean orderByLikeTimesDesc;
}