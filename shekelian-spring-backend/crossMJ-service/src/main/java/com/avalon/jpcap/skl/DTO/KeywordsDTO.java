package com.avalon.jpcap.skl.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordsDTO {
    /**
     * 关键词
     */
    private String keyword;
    /**
     * 关键词的数量
     */
    private Integer count;
}
