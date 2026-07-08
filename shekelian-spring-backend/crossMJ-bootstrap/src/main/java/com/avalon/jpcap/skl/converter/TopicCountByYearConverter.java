package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.TopicCountByYearDTO;
import com.avalon.jpcap.skl.vo.TopicCountByYearVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TopicCountByYearConverter {
    /**
     * 将 TopicCountByYearDTO 转换为 TopicCountByYearVO
     */
    public static TopicCountByYearVO convertToVO(TopicCountByYearDTO dto) {
        if (dto == null) return null;

        TopicCountByYearVO vo = new TopicCountByYearVO();
        vo.setYear(dto.getYear());
        vo.setTopicCount(dto.getTopicCount());
        return vo;
    }

    /**
     * 批量转换 TopicCountByYearDTO 列表 → TopicCountByYearVO 列表
     */
    public static List<TopicCountByYearVO> convertToVOList(List<TopicCountByYearDTO> dtoList) {
        return dtoList.stream()
                .map(TopicCountByYearConverter::convertToVO)
                .collect(Collectors.toList());
    }
}
