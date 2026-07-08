package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.SSCIInfoDTO;
import com.avalon.jpcap.skl.vo.SSCIAuthorVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class SSCIVOConverter {

    public static List<SSCIAuthorVO> convertToVOList(List<SSCIInfoDTO> dtos) {
        return IntStream.range(0, dtos.size())
                .mapToObj(index -> convertToVO(dtos.get(index), index + 1))
                .collect(Collectors.toList());
    }

    private static SSCIAuthorVO convertToVO(SSCIInfoDTO dto, int rank) {
        SSCIAuthorVO vo = new SSCIAuthorVO();
        vo.setRank(rank);
        vo.setName(dto.getName());
        vo.setPaperCountDisplay(dto.getPaperCount());
        return vo;
    }

    public static SSCIAuthorVO convert(SSCIInfoDTO dto) {
        SSCIAuthorVO vo = new SSCIAuthorVO();
        vo.setName(dto.getFirstAuthorName());
        vo.setFirstAuthor(dto.getFirstAuthorName());
        vo.setInstitution(dto.getOrganization());
        vo.setAuthors(dto.getAuthorList());
        vo.setPublishYear(dto.getYear());
        vo.setResearchDirection(dto.getResearchAreas());
        vo.setObjName(dto.getObjName());
        return vo;
    }

    public static List<SSCIAuthorVO> convertList(List<SSCIInfoDTO> dtos) {
        return dtos.stream().map(SSCIVOConverter::convert).collect(Collectors.toList());
    }
}
