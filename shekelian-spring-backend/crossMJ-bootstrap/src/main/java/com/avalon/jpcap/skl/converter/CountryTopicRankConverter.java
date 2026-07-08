package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.CountryTopicInfoDTO;
import com.avalon.jpcap.skl.vo.CountryTopicTopOrganizationVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CountryTopicRankConverter {
    /**
     * 将 CountryTopicInfoDTO 列表转换为 CountryTopicTopOrganizationVO 列表
     */
    public static List<CountryTopicTopOrganizationVO> convertToVOList(List<CountryTopicInfoDTO> dtos) {
        return IntStream.range(0, dtos.size())
                .mapToObj(index -> convertToVO(dtos.get(index), index + 1))
                .collect(Collectors.toList());
    }

    /**
     * 将单个 CountryTopicInfoDTO 转换为 CountryTopicTopOrganizationVO
     */
    private static CountryTopicTopOrganizationVO convertToVO(CountryTopicInfoDTO dto, int rank) {
        CountryTopicTopOrganizationVO vo =  new CountryTopicTopOrganizationVO();
        vo.setRank(rank);
        vo.setOrganizationName(dto.getOrganizationName());
        vo.setTopicCountDisplay(Integer.valueOf(dto.getTopicCount()));
        return vo;
    }
}
