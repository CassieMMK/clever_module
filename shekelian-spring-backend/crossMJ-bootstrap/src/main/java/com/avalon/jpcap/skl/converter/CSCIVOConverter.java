package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.CSCIInfoDTO;
import com.avalon.jpcap.skl.vo.CSCIAuthorVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CSCIVOConverter {

    public static List<CSCIAuthorVO> convertToVOList(List<CSCIInfoDTO> dtos) {
        return IntStream.range(0, dtos.size())
                .mapToObj(index -> convertToVO(dtos.get(index), index + 1))
                .collect(Collectors.toList());
    }

    private static CSCIAuthorVO convertToVO(CSCIInfoDTO dto, int rank) {
        CSCIAuthorVO vo = new CSCIAuthorVO();
        vo.setRank(rank);
        vo.setName(dto.getName());
        vo.setPaperCountDisplay(dto.getPaperCount());
        return vo;
    }

    public static CSCIAuthorVO convert(CSCIInfoDTO dto) {
        CSCIAuthorVO vo = new CSCIAuthorVO();
        vo.setName(dto.getFirstAuthorName());
        vo.setFirstAuthor(dto.getFirstAuthorName());
        vo.setInstitution(dto.getOrganization());
        vo.setAuthors(dto.getAuthorList());
        vo.setPublishYear(dto.getYear());
        vo.setResearchDirection(dto.getResearchDirect());
        vo.setObjName(dto.getObjName());
        return vo;
    }

    public static List<CSCIAuthorVO> convertList(List<CSCIInfoDTO> dtos) {
        return dtos.stream().map(CSCIVOConverter::convert).collect(Collectors.toList());
    }
}
