package com.avalon.jpcap.skl.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.*;

@Data
@ApiModel("论文查询请求参数")
public class SelectRequest {
    private String paperName;
    private String firstAuthor;
    private String organization;
    private String publishYear;
    private String researchField;
    private String paperType = "CSCI"; // 默认CSCI
    private Integer page = 0;         // 默认第0页
    private Integer size = 10;        // 默认每页10条
}