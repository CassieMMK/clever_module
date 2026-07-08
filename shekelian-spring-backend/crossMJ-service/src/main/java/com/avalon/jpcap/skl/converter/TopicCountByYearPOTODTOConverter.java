package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.repository.skl.TopicCountByYearPO;
import com.avalon.jpcap.skl.DTO.TopicCountByYearDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
@Component
public class TopicCountByYearPOTODTOConverter {

    /**
     * 将 TopicCountByYearPO 转换为 TopicCountByYearDTO
     */
    public static TopicCountByYearDTO convertToDTO(TopicCountByYearPO po) {
        if (po == null) return null;

        TopicCountByYearDTO dto = new TopicCountByYearDTO();
        // 直接使用 createTime 作为年份
        dto.setYear(po.getYear());
        dto.setTopicCount(po.getTopicCount());
        return dto;
    }

    /**
     * 批量转换 TopicCountByYearPO 列表 → TopicCountByYearDTO 列表
     */
    public static List<TopicCountByYearDTO> convertToDTOList(List<TopicCountByYearPO> poList) {
        return poList.stream()
                .map(TopicCountByYearPOTODTOConverter::convertToDTO)
                .collect(Collectors.toList());
    }
}
