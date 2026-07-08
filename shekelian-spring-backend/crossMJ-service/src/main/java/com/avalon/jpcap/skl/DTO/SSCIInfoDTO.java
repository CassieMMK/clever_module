package com.avalon.jpcap.skl.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "SSCI和HCI统计信息DTO")
@JsonInclude(value= JsonInclude.Include.NON_NULL)
public class SSCIInfoDTO {
    /**
     * 作者姓名
     */
    private String name;
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
    /**
     * 论文数量
     */
    private Integer paperCount;

}