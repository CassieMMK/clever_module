package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.repository.skl.TotalNumsCountPO;
import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import org.springframework.stereotype.Component;

@Component
public class TotalNumsConverter {
    /**
     * 将 TotalNumsCountPO 转换为 TotalNumsDTO
     */
    public static TotalNumsDTO convertToDTO(TotalNumsCountPO po) {
        if (po == null) {
            return null;
        }
        TotalNumsDTO dto = new TotalNumsDTO();
        dto.setCount(po.getCount());
        return dto;
    }


}
