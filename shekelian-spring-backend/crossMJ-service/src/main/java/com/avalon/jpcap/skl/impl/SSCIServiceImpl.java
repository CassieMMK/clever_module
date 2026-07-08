package com.avalon.jpcap.skl.impl;

import com.avalon.jpcap.infrastructure.skl.mapper.PersonMapper;
import com.avalon.jpcap.repository.skl.AuthorCountPO;
import com.avalon.jpcap.repository.skl.PersonPO;
import com.avalon.jpcap.skl.DTO.SSCIInfoDTO;
import com.avalon.jpcap.skl.service.SSCIService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.avalon.jpcap.infrastructure.skl.mapper.SSCIMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class SSCIServiceImpl implements SSCIService {
    @Autowired
    private SSCIMapper SSCIMapper;
    @Autowired
    private PersonMapper personMapper;

    @Override
    public List<SSCIInfoDTO> getSSCItop5InfoDto() {
        // 1. 获取原始PO数据
        List<AuthorCountPO> authorCounts = SSCIMapper.findTop5AuthorCounts();
        List<Integer> authorIds = authorCounts.stream()
                .map(AuthorCountPO::getId)
                .collect(Collectors.toList());

        // 2. 批量查询作者信息PO
        Map<Integer, PersonPO> authorNameMap = personMapper.selectNameByIds(authorIds);

        // 3. PO转换为DTO
        return authorCounts.stream()
                .map(po -> convertToDTO(po, authorNameMap))
                .collect(Collectors.toList());
    }

    private SSCIInfoDTO convertToDTO(AuthorCountPO countPO, Map<Integer, PersonPO> nameMap) {
        SSCIInfoDTO dto = new SSCIInfoDTO();

        // 修复1：正确处理nameMap可能为null的情况
        PersonPO person = nameMap != null ? nameMap.get(countPO.getId()) : null;
        dto.setName(person != null ? person.getName() : "未知作者");

        dto.setPaperCount(countPO.getPaperCount());
        return dto;
    }



}
