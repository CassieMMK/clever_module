package com.avalon.jpcap.repository.po;

import lombok.Data;

/**
 * @author DingHaoLun
 * @since 2023-05-13 20:13
 **/
@Data
public class PromptModelPO extends BasePO {

    /**
     * 主键id
     */
    private Long id;

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