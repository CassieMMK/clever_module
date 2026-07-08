package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseDtoDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author DingHaoLun
 * @since 2023-05-15 14:03
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromptModelDto extends BaseDtoDomain {

    /**
     * 卡片id
     */
    private Long promptModelId;

    /**
     * 创建者id
     */
    private Long userId;

    /**
     * 封装提示词
     */
    private String prompt;

    /**
     * 风格示例图片
     */
    private String modelImgUrl;

    /**
     * 被使用过次数
     */
    private Integer usedTimes;

    /**
     * 被点赞次数
     */
    private Integer likeTimes;
}