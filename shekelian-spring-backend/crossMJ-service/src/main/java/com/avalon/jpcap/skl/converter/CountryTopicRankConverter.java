package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.repository.skl.OrganizationCountPO;
import com.avalon.jpcap.skl.DTO.CountryTopicInfoDTO;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.stream.Collectors;
@Component
public class CountryTopicRankConverter {
    /**
     * 将 OrganizationCountPO 转换为 CountryTopicInfoDTO（未设置 name 字段）
     */
    public static CountryTopicInfoDTO convertToDTO(OrganizationCountPO po) {
        if (po == null) return null;

        CountryTopicInfoDTO dto = new CountryTopicInfoDTO();
        dto.setOrganizationId(po.getId());
        dto.setTopicCount(po.getTopicCount());
        // organizationName 字段稍后通过其他方法设置
        return dto;
    }

    /**
     * 批量转换 PO 列表 → DTO 列表
     */
    public static List<CountryTopicInfoDTO> convertToDTOList(List<OrganizationCountPO> poList) {
        return poList.stream()
                .map(CountryTopicRankConverter::convertToDTO)
                .collect(Collectors.toList());
    }
}
