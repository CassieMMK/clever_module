package com.avalon.jpcap.skl.impl;

import com.avalon.jpcap.infrastructure.skl.mapper.*;
import com.avalon.jpcap.repository.skl.CSCIPO;
import com.avalon.jpcap.repository.skl.SSCIPO;
import com.avalon.jpcap.skl.DTO.*;
import com.avalon.jpcap.skl.converter.CSCIDTOConverter;
import com.avalon.jpcap.skl.converter.SSCIDTOConverter;
import com.avalon.jpcap.skl.service.SelectService;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SelectServiceimpl implements SelectService {

    @Autowired
    private PersonMapper personMapper;
    @Autowired
    private CSCIMapper csciMapper;
    @Autowired
    private SSCIMapper ssciMapper;
    @Autowired
    private CSCIDTOConverter csciDtoConverter;
    @Autowired
    private SSCIDTOConverter ssciDtoConverter;

    @Override
    public SelectDTO selectPapers(SelectRequestDTO request) {
        SelectDTO result = new SelectDTO();
        result.setName("论文查询结果");

        if ("CSCI".equals(request.getPaperType())) {
            // 使用PageHelper进行分页查询
            com.github.pagehelper.Page<CSCIPO> csciPage = PageHelper.startPage(
                    request.getPage(),
                    request.getSize()
            ).doSelectPage(() ->
                    csciMapper.selectCSCIArticles(
                            request.getPaperName(),
                            request.getFirstAuthor(),
                            request.getOrganization(),
                            request.getPublishYear() != null ? Integer.parseInt(request.getPublishYear()) : null,
                            request.getResearchField()
                    )
            );

            // 转换为DTO
            PaperPageDTO<CSCIInfoDTO> csciDtoPage = PaperPageDTO.of(csciPage, csciDtoConverter::convertToDTO);
            result.setCsciResult(csciDtoPage);
        }

        if ("SSCI&HCI".equals(request.getPaperType())) {
            // 同样处理SSCI论文
            com.github.pagehelper.Page<SSCIPO> ssciPage = PageHelper.startPage(
                    request.getPage(),
                    request.getSize()
            ).doSelectPage(() ->
                    ssciMapper.selectSSCIArticles(
                            request.getPaperName(),
                            request.getFirstAuthor(),
                            request.getOrganization(),
                            request.getPublishYear() != null ? Integer.parseInt(request.getPublishYear()) : null,
                            request.getResearchField()
                    )
            );

            PaperPageDTO<SSCIInfoDTO> ssciDtoPage = PaperPageDTO.of(ssciPage, ssciDtoConverter::convertToDTO);
            result.setSsciResult(ssciDtoPage);
        }

        // 设置通用分页信息
        if (result.getCsciResult() != null) {
            result.setPagination(result.getCsciResult());
        } else if (result.getSsciResult() != null) {
            result.setPagination(result.getSsciResult());
        } else {
            // 如果没有查询结果，创建空的分页信息
            PaperPageDTO<?> pagination = new PaperPageDTO<>();
            pagination.setCurrent(request.getPage());
            pagination.setPageSize(request.getSize());
            pagination.setTotal(0L);
            result.setPagination(pagination);
        }

        return result;
    }

    private <PO, DTO> PaperPageDTO<DTO> convertPage(Page<PO> page, Function<PO, DTO> converter) {
        PaperPageDTO<DTO> dto = new PaperPageDTO<>();
        dto.setRecords(page.getContent().stream()
                .map(converter)
                .collect(Collectors.toList()));
        dto.setCurrent(page.getNumber());
        dto.setPageSize(page.getSize());
        dto.setTotal(page.getTotalElements());
        dto.setPages(page.getTotalPages());
        return dto;
    }
}