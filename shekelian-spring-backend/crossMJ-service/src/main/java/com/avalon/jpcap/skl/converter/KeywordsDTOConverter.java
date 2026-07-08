package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.repository.skl.CSCIPO;
import com.avalon.jpcap.skl.DTO.KeywordsDTO;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class KeywordsDTOConverter {

    /**
     * 将单个 CSCIPO 对象的 keywords 字段转换为 KeywordsDTO 列表
     *
     * @param po CSCIPO 对象
     * @return KeywordsDTO 对象列表
     */
    public List<KeywordsDTO> convertToDTO(CSCIPO po) {
        if (po == null || po.getKeywords() == null || po.getKeywords().isEmpty()) {
            return null;
        }

        // 去掉每行最后的分号并按分号拆分关键词
        String[] keywordsArray = po.getKeywords().replaceAll("；$", "").split("；");

        // 将每个关键词转换成 KeywordsDTO，并且默认每个关键词初始计数为 1
        return Arrays.stream(keywordsArray)
                .map(keyword -> {
                    KeywordsDTO dto = new KeywordsDTO();
                    dto.setKeyword(keyword);
                    dto.setCount(1);  // 初始时每个关键词的数量设置为 1
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 将多个 CSCIPO 对象的 keywords 字段转换为 KeywordsDTO 对象列表
     *
     * @param pos CSCIPO 对象列表
     * @return KeywordsDTO 对象列表
     */
    public List<KeywordsDTO> convertToDTOList(List<CSCIPO> pos) {
        return pos.stream()
                .flatMap(po -> convertToDTO(po).stream()) // 扁平化列表，将每个 CSCIPO 转换成多个 KeywordsDTO
                .collect(Collectors.toList());
    }

    /**
     * 将多个关键词字符串转换为 KeywordsDTO 对象列表
     *
     * @param keywords 关键词列表
     * @return KeywordsDTO 对象列表
     */
    public List<KeywordsDTO> convertToDTOListFromStrings(List<String> keywords) {
        return keywords.stream()
                .map(keyword -> {
                    KeywordsDTO dto = new KeywordsDTO();
                    dto.setKeyword(keyword);
                    dto.setCount(1);  // 初始时每个关键词的数量设置为 1
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
