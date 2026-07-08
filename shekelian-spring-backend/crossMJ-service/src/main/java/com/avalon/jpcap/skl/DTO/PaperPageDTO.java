package com.avalon.jpcap.skl.DTO;

import com.github.pagehelper.Page;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ApiModel("分页结果")
public class PaperPageDTO<T> {
    private List<T> records;
    private int current;
    private int pageSize;
    private long total;
    private int pages;

    public static <PO, DTO> PaperPageDTO<DTO> of(Page<PO> page, Function<PO, DTO> converter) {
        PaperPageDTO<DTO> dto = new PaperPageDTO<>();
        dto.setRecords(page.getResult().stream()
                .map(converter)
                .collect(Collectors.toList()));
        dto.setCurrent(page.getPageNum());
        dto.setPageSize(page.getPageSize());
        dto.setTotal(page.getTotal());
        dto.setPages(page.getPages());
        return dto;
    }
}