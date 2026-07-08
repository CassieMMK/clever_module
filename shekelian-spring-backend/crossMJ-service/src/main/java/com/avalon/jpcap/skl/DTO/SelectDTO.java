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
@ApiModel(description = "CSSCI统计信息DTO")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class SelectDTO {
    private String name;
    private PaperPageDTO<CSCIInfoDTO> csciResult;  // 修改为PaperPageDTO类型
    private PaperPageDTO<SSCIInfoDTO> ssciResult;  // 修改为PaperPageDTO类型
    private PaperPageDTO<?> pagination;  // 通用分页信息
}