package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.repository.skl.AuthorCountPO;
import com.avalon.jpcap.repository.skl.SSCIPO;
import com.avalon.jpcap.skl.DTO.SSCIInfoDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SSCIDTOConverter {
    /**
     * 将 AuthorCountPO 转换为 CSCIInfoDTO（未设置 name 字段）
     */


    public static SSCIInfoDTO CountPOconvertToDTO(AuthorCountPO po) {
        if (po == null) return null;
        SSCIInfoDTO dto = new SSCIInfoDTO();
        dto.setPaperCount(po.getPaperCount());
        // name 字段稍后通过其他方法设置
        return dto;
    }

    /**
     * 批量转换 PO 列表 → DTO 列表
     */
    public static List<SSCIInfoDTO> convertToDTOList(List<AuthorCountPO> poList) {
        return poList.stream()
                .map(SSCIDTOConverter::CountPOconvertToDTO)
                .collect(Collectors.toList());
    }

    /**
     *将PO转换为DTO并且设置对应的name
     */
    // 单对象转换
    public SSCIInfoDTO convertToDTO(SSCIPO po) {
        SSCIInfoDTO dto = new SSCIInfoDTO();
        dto.setFirstAuthorName(po.getFirstAuthorName());
        dto.setOrganization(po.getOrganization());
        dto.setAuthorList(po.getAuthorList());
        dto.setYear(po.getYear());
        dto.setResearchAreas(po.getResearchAreas());
        dto.setObjName(po.getObjName());
        return dto;
    }

    /**
     * 批量转换
     */
    public List<SSCIInfoDTO> convertToDTOS(List<SSCIPO> pos) {
        if (pos == null || pos.isEmpty()) {
            return Collections.emptyList();
        }

        return pos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<SSCIInfoDTO> convertToSSCIDTOList(List<SSCIPO> poList) {
        if (poList == null) return Collections.emptyList();
        return poList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}