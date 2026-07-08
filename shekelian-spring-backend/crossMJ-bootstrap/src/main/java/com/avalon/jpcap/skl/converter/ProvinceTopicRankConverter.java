package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.ProvinceTopicInfoDTO;
import com.avalon.jpcap.skl.vo.ProvinceTopicTopOrganizationVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ProvinceTopicRankConverter {
    /**
     * 将 ProvinceTopicInfoDTO 列表转换为 ProvinceTopicTopOrganizationVO 列表
     */
    public static List<ProvinceTopicTopOrganizationVO> convertToVOList(List<ProvinceTopicInfoDTO> dtos) {
        return IntStream.range(0, dtos.size())
                .mapToObj(index -> convertToVO(dtos.get(index), index + 1))
                .collect(Collectors.toList());
    }

    /**
     * 将单个 ProvinceTopicInfoDTO 转换为 ProvinceTopicTopOrganizationVO
     */
    private static ProvinceTopicTopOrganizationVO convertToVO(ProvinceTopicInfoDTO dto, int rank) {
        ProvinceTopicTopOrganizationVO vo = new ProvinceTopicTopOrganizationVO();
        vo.setRank(rank);
        vo.setOrganizationName(dto.getOrganizationName());
        vo.setTopicCountDisplay(Integer.valueOf(dto.getTopicCount()));
        return vo;
    }
}
