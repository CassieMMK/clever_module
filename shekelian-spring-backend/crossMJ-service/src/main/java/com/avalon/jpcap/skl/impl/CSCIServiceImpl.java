package com.avalon.jpcap.skl.impl;

import com.avalon.jpcap.infrastructure.skl.mapper.CSCIMapper;
import com.avalon.jpcap.infrastructure.skl.mapper.PersonMapper;
import com.avalon.jpcap.repository.skl.AuthorCountPO;
import com.avalon.jpcap.repository.skl.PersonPO;
import com.avalon.jpcap.skl.DTO.CSCIInfoDTO;
import com.avalon.jpcap.skl.DTO.KeywordsDTO;
import com.avalon.jpcap.skl.converter.KeywordsDTOConverter;
import com.avalon.jpcap.skl.service.CSCIService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CSCIServiceImpl implements CSCIService {
    @Autowired
    private CSCIMapper CSCIMapper;
    @Autowired
    private PersonMapper personMapper;
    @Autowired
    private KeywordsDTOConverter keywordsDTOConverter;

    @Autowired
    private CSCIService self; // 注入自身代理
    // 1. 定义一个 SLF4J Logger 实例
    private static final Logger logger = LoggerFactory.getLogger(CSCIServiceImpl.class);

    @Override
    public List<CSCIInfoDTO> getCSCItop5InfoDto() {
        // 1. 获取原始PO数据
        List<AuthorCountPO> authorCounts = CSCIMapper.findTop5AuthorCounts();
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

    private CSCIInfoDTO convertToDTO(AuthorCountPO countPO, Map<Integer, PersonPO> nameMap) {
        CSCIInfoDTO dto = new CSCIInfoDTO();

        // 修复1：正确处理nameMap可能为null的情况
        PersonPO person = nameMap != null ? nameMap.get(countPO.getId()) : null;
        dto.setName(person != null ? person.getName() : "未知作者");

        dto.setPaperCount(countPO.getPaperCount());
        return dto;
    }

    /**
     * 统计单个关键词的数量的具体实现
     */
    @Override
    // 统计所有论文的关键词，并返回唯一关键词的数量
    @Cacheable(value = "keywords", key = "#root.methodName")  // 使用缓存，key 基于方法名称
    public List<KeywordsDTO> getKeywordStatistics() {
        // 2. 在方法体开始处添加明确的“缓存未命中”日志
        logger.info(">>> [缓存未命中] getKeywordStatistics() 方法体被执行，正在从数据库获取数据...");

        List<Map<String, Object>> keywordCounts = CSCIMapper.findKeywordStatistics();
        List<KeywordsDTO> keywordDTOs = new ArrayList<>();
        for (Map<String, Object> entry : keywordCounts) {
            String keyword = (String) entry.get("keyword");
            Number countNumber = (Number) entry.get("count");
            Integer count = countNumber.intValue();
            keywordDTOs.add(new KeywordsDTO(keyword, count));
        }
        logger.info("getKeywordStatistics() 方法执行完毕，获取到 {} 个关键词统计。", keywordDTOs.size());
        return keywordDTOs;

    }
    /**
     * 获取topN关键词
     */
    @Override
    public List<KeywordsDTO> getTopNKeywordStatistics(int n) {
        logger.info("方法 getTopNKeywordStatistics() 被执行，请求 Top N={}", n);

        if (n <= 0) {
            logger.warn("请求的 N ({}) 无效 (小于等于0)，返回空列表。", n);
            return Collections.emptyList();
        }

        // 此调用会利用 getKeywordStatistics() 的缓存
        // 如果 getKeywordStatistics() 缓存命中，它内部的日志（包括上面的“缓存未命中”日志）将不会打印
        List<KeywordsDTO> allKeywords = self.getKeywordStatistics(); // 通过代理调用

        if (allKeywords.isEmpty()) {
            logger.info("所有关键词列表为空，为 Top N 返回空列表。");
            return Collections.emptyList();
        }

        List<KeywordsDTO> topNKeywords = allKeywords.stream()
                .limit(n)
                .collect(Collectors.toList());
        logger.info("getTopNKeywordStatistics 方法执行完毕，从总共 {} 个关键词中，返回了 {} 个。", allKeywords.size(), topNKeywords.size());
        return topNKeywords;
    }

}

/*
引入自身代理的原因：
问题点：当一个 bean 的方法调用同一个 bean 的另一个方法时（即 this.anotherMethod()），
这个调用是直接在原始对象实例上发生的，它不会经过 Spring 的 AOP 代理。
因此，getKeywordStatistics() 方法上的 @Cacheable 注解（以及其他AOP注解如 @Transactional）在这种内部调用中不会生效。

所以，即使第一次调用 getTopNKeywordStatistics() 时，getKeywordStatistics() 执行了，
并且理论上它的结果应该被缓存了，但第二次调用 getTopNKeywordStatistics() 时，它再次内部调用 getKeywordStatistics()，
这个内部调用仍然绕过了缓存检查，直接执行了 getKeywordStatistics() 的方法体，于是又打印了“缓存未命中”。
 */