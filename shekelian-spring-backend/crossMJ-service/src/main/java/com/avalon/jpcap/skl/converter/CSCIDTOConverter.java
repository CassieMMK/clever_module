package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.repository.skl.AuthorCountPO;
import com.avalon.jpcap.repository.skl.CSCIPO;
import com.avalon.jpcap.skl.DTO.CSCIInfoDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class CSCIDTOConverter {

        public CSCIInfoDTO convertToDTO(AuthorCountPO po) {
            if (po == null) return null;
            CSCIInfoDTO dto = new CSCIInfoDTO();
            dto.setPaperCount(po.getPaperCount());
            return dto;
        }


        public CSCIInfoDTO convertToDTO(CSCIPO po) {
            CSCIInfoDTO dto = new CSCIInfoDTO();
            dto.setFirstAuthorName(po.getFirstAuthorName());
            dto.setOrganization(po.getOrganization());
            dto.setAuthorList(po.getAuthorList());
            dto.setYear(po.getYear());
            dto.setResearchDirect(po.getResearchDirect());
            dto.setObjName(po.getObjName());
            return dto;
        }

        public List<CSCIInfoDTO> convertToDTOS(List<CSCIPO> pos) {
            if (pos == null || pos.isEmpty()) {
                return Collections.emptyList();
            }
            return pos.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }

    public List<CSCIInfoDTO> convertToDTOList(List<CSCIPO> poList) {
        if (poList == null) return Collections.emptyList();
        return poList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}

