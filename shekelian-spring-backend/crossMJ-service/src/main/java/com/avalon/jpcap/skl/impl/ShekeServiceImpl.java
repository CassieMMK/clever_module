package com.avalon.jpcap.skl.impl;

import com.avalon.jpcap.infrastructure.skl.mapper.*;
import com.avalon.jpcap.repository.skl.AuthorCountPO;
import com.avalon.jpcap.repository.skl.PersonPO;
import com.avalon.jpcap.repository.skl.TotalNumsCountPO;
import com.avalon.jpcap.skl.DTO.CSCIInfoDTO;
import com.avalon.jpcap.skl.DTO.TotalNumsDTO;
import com.avalon.jpcap.skl.converter.TotalNumsConverter;
import com.avalon.jpcap.skl.service.ShekeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 仅作统一文件名使用
 */

/*
implements作用如下：
定义实现关系：明确指出某个类是某个接口的实现者。
强制实现方法：确保实现类提供了接口中所有方法的实现，保证了接口定义的契约。
多态支持：允许接口类型的变量引用其任意实现类的对象，这是Java多态性的体现。
*/

//TODO:现在只是定义了简单的返回查询结果的方法，后续对于同一个表的方法要进行封装


@Service
public class ShekeServiceImpl implements ShekeService {

    @Resource
    ShekeService shekeService;

    /*
    四川社科团队数量
     */
    @Autowired
    private SocialScienceGroupInfoMapper shekeTeamsInfoMapper;

    @Override
    public int shekeTeamsNums() {
        return shekeTeamsInfoMapper.shekeTeamsNums();
    }

    /*
    SSCI和HCI论文总数
     */
    @Autowired
    private SSCIPaperInfoMapper scipaperInfoMapper;

    @Override
    public TotalNumsDTO ssciTotalNums() {
        TotalNumsCountPO po =scipaperInfoMapper.ssciTotalNums();
        TotalNumsDTO dto = TotalNumsConverter.convertToDTO(po);
        return dto;
    }

    /*
    CSCI论文总数
     */
    @Autowired
    private CSCIMapper csciMapper;
    @Autowired
    private PersonMapper personMapper;

    @Override
    public TotalNumsDTO csciTotalNums() {
        TotalNumsCountPO po =  csciMapper.csciTotalNums();
        TotalNumsDTO dto =TotalNumsConverter.convertToDTO(po);
        return dto;
    }

    @Override
    public List<CSCIInfoDTO> getTop5AuthorsWithNames() {
        // 1. 获取原始数据（已内置空值保护）
        List<AuthorCountPO> authorPOs = csciMapper.findTop5AuthorCounts();

        // 2. 提取有效ID并批量查询（优化性能）
        Map<Integer, PersonPO> personMap = fetchPersonMap(authorPOs);

        // 3. 转换DTO并填充数据
        return convertToDTOs(authorPOs, personMap);
    }

    private Map<Integer, PersonPO> fetchPersonMap(List<AuthorCountPO> authorPOs) {
        List<Integer> validIds = authorPOs.stream()
                .map(AuthorCountPO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return validIds.isEmpty() ?
                Collections.emptyMap() :
                personMapper.selectNameByIds(validIds);
    }

    private List<CSCIInfoDTO> convertToDTOs(List<AuthorCountPO> poList, Map<Integer, PersonPO> personMap) {
        return poList.stream().map(po -> {
            CSCIInfoDTO dto = new CSCIInfoDTO();
            // 设置基础字段
            dto.setPaperCount(po.getPaperCount());
            PersonPO person = personMap.get(po.getId());
            dto.setName(person != null ? person.getName() : "未知作者");
            return dto;
        }).collect(Collectors.toList());
    }
}
