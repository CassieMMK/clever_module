package com.avalon.jpcap.skl.impl;

import com.avalon.jpcap.infrastructure.skl.mapper.SocialScienceGroupInfoMapper;
import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import com.avalon.jpcap.skl.service.SocialScienceGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialScienceGroupServiceImpl implements SocialScienceGroupService {
    @Autowired
    private SocialScienceGroupInfoMapper socialScienceGroupInfoMapper;

    /*
    1.返回社科团队总数
     */
    @Override
    public TotalNumsDTO countSocialScienceGroupDto() {
        //1. 获取原始数据（备注：这里暂时不需要定义PO，因为没有必要，我统计的是行数，不属于列名，不需要哪个属性）
        Integer count = socialScienceGroupInfoMapper.shekeTeamsNums();

        //2. PO 转 DTO （service层数据的形式应该已经转为dto）
        TotalNumsDTO totalNumsDTO = new TotalNumsDTO();
        totalNumsDTO.setCount(count != null ? count.intValue() : 0); // 确保 count 不为 null
        return totalNumsDTO;
    }
}
