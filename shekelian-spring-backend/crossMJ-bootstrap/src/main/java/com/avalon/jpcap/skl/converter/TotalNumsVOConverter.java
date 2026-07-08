package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import com.avalon.jpcap.skl.vo.TotalNumsVO;
import org.springframework.stereotype.Component;

@Component
public class TotalNumsVOConverter {
    /**
     * 将 TotalNumsDTO 转换为 TotalNumsVO
     */
    public TotalNumsVO convertToVO(TotalNumsDTO dto) {
        if (dto == null) {
            return null;
        }
        TotalNumsVO vo = new TotalNumsVO();
        vo.setCount(dto.getCount());
        return vo;
    }
}
