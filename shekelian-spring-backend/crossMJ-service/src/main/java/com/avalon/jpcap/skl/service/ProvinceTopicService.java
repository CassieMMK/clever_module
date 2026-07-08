package com.avalon.jpcap.skl.service;

import com.avalon.jpcap.skl.DTO.ProvinceTopicInfoDTO;
import com.avalon.jpcap.skl.DTO.TopicCountByYearDTO;
import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ProvinceTopicService {
    /**
     * 历年省课题统计（echar）
     */
    List<TopicCountByYearDTO> getTopicCountByYeardto();

    /**
     * 获取省课题总数
     */
    TotalNumsDTO countProvinceTopicsDto();

    /**
     * 获取省课题数量为前五的单位
     */
    List<ProvinceTopicInfoDTO> getProvincetop5InfoDto();
}
