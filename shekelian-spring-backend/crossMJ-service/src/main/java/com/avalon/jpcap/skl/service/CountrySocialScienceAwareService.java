package com.avalon.jpcap.skl.service;

import com.avalon.jpcap.skl.DTO.CountryTopicInfoDTO;
import com.avalon.jpcap.skl.DTO.TopicCountByYearDTO;
import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CountrySocialScienceAwareService {
    /**
     * 历年国社科奖统计（echar）
     */
    List<TopicCountByYearDTO> national_rewards_statistic();

    /**
     * 获国家社科课题前5位机关
     */
    List<CountryTopicInfoDTO> getCountrytop5InfoDto();

    /**
     * 国家社科课题总数
     */
    TotalNumsDTO countCountryTopicsDto();
}
