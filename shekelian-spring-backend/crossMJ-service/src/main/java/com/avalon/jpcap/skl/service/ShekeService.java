package com.avalon.jpcap.skl.service;

import com.avalon.jpcap.skl.DTO.CSCIInfoDTO;
import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 仅作统一文件名使用
 */
@Service
public interface ShekeService {

    /**
     * 四川社科团队数量
     */
    int shekeTeamsNums();

    /**
     * SSCI和HCI论文总数
     */
    TotalNumsDTO ssciTotalNums();

    /**
     * 获取出现次数前五的作者及其姓名
     * @return 包含作者姓名和出现次数的DTO列表
     */
    List<CSCIInfoDTO> getTop5AuthorsWithNames();
    /**
     * CSCI论文总数
     */
    TotalNumsDTO csciTotalNums();


}
