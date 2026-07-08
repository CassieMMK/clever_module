package com.avalon.jpcap.skl.service;

import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import org.springframework.stereotype.Service;

@Service
public interface SocialScienceAwareService {

    // 社科获奖总数
    TotalNumsDTO countSocialScienceAwareDto();
}
