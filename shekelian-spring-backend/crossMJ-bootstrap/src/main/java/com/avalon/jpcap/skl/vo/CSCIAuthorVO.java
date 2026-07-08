package com.avalon.jpcap.skl.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CSSCI作者统计展示VO")
@JsonInclude(value= JsonInclude.Include.NON_NULL)
public class CSCIAuthorVO {
    private Integer rank;              // 作者排名
    private String Name;               // 作者姓名
    private String firstAuthor;        // 第一作者（DTO 中的 firstAuthorName）
    private String institution;        // 机构（DTO 中的 organization）
    private String authors;      // 全部作者（DTO 中的 authorList）
    private Integer publishYear;       // 发表年份（DTO 中的 year）
    private String researchDirection;  // 研究方向（DTO 中的 researchDirect）
    private String objName;         // 论文名（DTO 中的 objName）
    private Integer paperCountDisplay;  // 论文数量（DTO 中的 paperCount）
}
