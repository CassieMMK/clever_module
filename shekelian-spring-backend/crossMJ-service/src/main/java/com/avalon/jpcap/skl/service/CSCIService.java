package com.avalon.jpcap.skl.service;


import com.avalon.jpcap.skl.DTO.CSCIInfoDTO;
import com.avalon.jpcap.skl.DTO.KeywordsDTO;

import java.util.List;

public interface CSCIService {
    /*
     **查询排名前五的Cssic作者
     */
    List<CSCIInfoDTO> getCSCItop5InfoDto();

    /**
     * 统计单个关键词的数量
     */
    List<KeywordsDTO> getKeywordStatistics();

    // 获取 Top N 关键词的新方法
    List<KeywordsDTO> getTopNKeywordStatistics(int n);

}
