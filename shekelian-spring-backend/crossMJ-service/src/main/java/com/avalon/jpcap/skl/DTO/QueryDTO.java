package com.avalon.jpcap.skl.DTO;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "查询信息DTO")
public class QueryDTO {
    String SelectType;
    String name;
    String paperType;
    private int pageNum = 1;
    private int pageSize = 10;
}
