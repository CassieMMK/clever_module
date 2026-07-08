package com.avalon.jpcap.repository.skl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CSCIPO {
    /**
     * 作者id
     */
    private Integer id;
    /**
     * 第一作者id
     */
    private Integer firstAuthorId;
    /**
     * 第一作者名
     */
    private String firstAuthorName;
    /**
     * 机构名
     */
    private String organization;
    /**
     * 全部作者
     */
    private String authorList;
    /**
     * 发表年份
     */
    private Integer year;
    /**
     * 研究方向
     */
    private String researchDirect;
    /**
     * 论文名
     */
    private String objName;

    /**
     * 关键词
     */
    private String keywords;
}