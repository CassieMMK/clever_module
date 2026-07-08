package com.avalon.jpcap.skl.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "查询信息VO")
@JsonInclude(value= JsonInclude.Include.NON_NULL)
public class SelectVO {
    private String name;
    private List<CSCIAuthorVO> csciList;
    private List<SSCIAuthorVO> ssciList;
    private PaginationVO pagination;
}