package com.avalon.jpcap.repository.skl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSCIPO {
    /**
     * id
     */
    private Integer id;
    /**
     * 第一作者id
     */
    private Integer firstAuthorId;
    /**
     * 第一作者姓名
     */
    private String firstAuthorName;
    /**
     * 机构id
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
    private String researchAreas;
    /**
     * 论文名
     */
    private String objName;


}