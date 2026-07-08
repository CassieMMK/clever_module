package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.repository.skl.OrganizationCountPO;
import com.avalon.jpcap.skl.DTO.ProvinceTopicInfoDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
@Component
public class ProvinceTopicRankConverter {
    /**
     * 将 OrganizationCountPO 转换为 ProvinceTopicInfoDTO（未设置 name 字段）
     */
    public static ProvinceTopicInfoDTO convertToDTO(OrganizationCountPO po) {
        if (po == null) return null;

        ProvinceTopicInfoDTO dto = new ProvinceTopicInfoDTO();
        dto.setOrganizationId(po.getId());
        dto.setTopicCount(po.getTopicCount());
        // organizationName 字段稍后通过其他方法设置
        return dto;
    }

    /**
     * 批量转换 PO 列表 → DTO 列表
     */
    public static List<ProvinceTopicInfoDTO> convertToDTOList(List<OrganizationCountPO> poList) {
        return poList.stream()
                .map(ProvinceTopicRankConverter::convertToDTO)
                .collect(Collectors.toList());
    }
}
