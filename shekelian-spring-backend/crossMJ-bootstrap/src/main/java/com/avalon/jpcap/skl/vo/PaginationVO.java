package com.avalon.jpcap.skl.vo;

import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@ApiModel("分页信息VO")
@NoArgsConstructor
public class PaginationVO {
    private int currentPage;
    private int pageSize;
    private long totalCount;

    public PaginationVO(int currentPage, int pageSize, long totalCount) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }
}