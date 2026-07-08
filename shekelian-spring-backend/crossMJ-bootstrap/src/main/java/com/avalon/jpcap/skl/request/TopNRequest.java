package com.avalon.jpcap.skl.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopNRequest {
    @Min(value = 1, message = "n 必须是大于0的整数")// 添加校验，确保 n 是正数
    @NotBlank(message = "n不能为空")
    @ApiModelProperty(value = "查询主体类型", example = "50", required = true)
    private int n; // 代表要获取的排名靠前记录的数量
}
