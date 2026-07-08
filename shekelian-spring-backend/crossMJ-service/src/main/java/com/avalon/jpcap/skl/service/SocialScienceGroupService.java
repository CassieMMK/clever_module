package com.avalon.jpcap.skl.service;

import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import org.springframework.stereotype.Service;

@Service
public interface SocialScienceGroupService {

    // 四川省社科团队总数
    TotalNumsDTO countSocialScienceGroupDto();
}
