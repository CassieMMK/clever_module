package com.avalon.jpcap.skl.impl;

import com.avalon.jpcap.infrastructure.skl.mapper.CountryTopicInfoMapper;
import com.avalon.jpcap.infrastructure.skl.mapper.OrganizationMapper;
import com.avalon.jpcap.repository.skl.OrganizationCountPO;
import com.avalon.jpcap.repository.skl.OrganizationPO;
import com.avalon.jpcap.repository.skl.TopicCountByYearPO;
import com.avalon.jpcap.skl.DTO.CountryTopicInfoDTO;
import com.avalon.jpcap.skl.DTO.TopicCountByYearDTO;
import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import com.avalon.jpcap.skl.converter.TopicCountByYearPOTODTOConverter;
import com.avalon.jpcap.skl.service.CountrySocialScienceAwareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CountrySocialScienceAwareServiceImpl implements CountrySocialScienceAwareService {

    @Resource
    CountrySocialScienceAwareService countrySocialScienceAwareService;

    @Autowired
    private CountryTopicInfoMapper countryTopicInfoMapper;

    @Autowired
    private OrganizationMapper organizationMapper;

    /**
     * 历年国社科奖统计（echar）
     */
    @Override  //覆盖父类方法
    public List<TopicCountByYearDTO> national_rewards_statistic() {
        List<TopicCountByYearPO> po = countryTopicInfoMapper.countTopicsByYear();
        List<TopicCountByYearDTO> dto = TopicCountByYearPOTODTOConverter.convertToDTOList(po);
        return dto;
    }


    /*
    国社科课题总数
     */
    @Override
    public TotalNumsDTO countCountryTopicsDto() {
        //1. 获取原始数据（备注：这里暂时不需要定义PO，因为没有必要，我统计的是行数，不属于列名）
        Integer count = countryTopicInfoMapper.countCountryTopics();

        //2. PO 转 DTO （service层数据的形式应该已经转为dto）
        TotalNumsDTO totalNumsDTO = new TotalNumsDTO();
        totalNumsDTO.setCount(count != null ? count.intValue() : 0); // 确保 count 不为 null
        return totalNumsDTO;
    }


    /*
    获国家社科课题前5位机关
     */
    @Override
    public List<CountryTopicInfoDTO> getCountrytop5InfoDto() {
        // 1. 获取原始PO数据
        List<OrganizationCountPO> organizationCounts = countryTopicInfoMapper.findTop5OrganizationCounts();
        List<Integer> organizationIds = organizationCounts.stream()
                .map(OrganizationCountPO::getId)
                .collect(Collectors.toList());
        System.out.println("Org IDs:" + organizationIds);// 输出 authorIds，检查是否有值

        // 2. 批量查询单位信息PO (假设我们也需要获取单位名称等信息)
        Map<Integer, OrganizationPO> organizationNameMap = organizationMapper.selectUnit_NameByIDS(organizationIds);

        // 3. PO转换为DTO
        return organizationCounts.stream()
                .map(po -> convertToDTO(po, organizationNameMap))
                .collect(Collectors.toList());
    }


    private CountryTopicInfoDTO convertToDTO(OrganizationCountPO countPO, Map<Integer, OrganizationPO> nameMap) {
        CountryTopicInfoDTO dto = new CountryTopicInfoDTO();
        dto.setOrganizationId(countPO.getId());

        // 获取单位名称（假设我们可以从名称映射中获取）
        OrganizationPO organizationPO = nameMap != null ? nameMap.get(countPO.getId()) : null;
        dto.setOrganizationName(organizationPO != null ? organizationPO.getUnit_name() : "未知单位");

        dto.setTopicCount(countPO.getTopicCount());
        return dto;
    }

}
