package com.avalon.jpcap.skl.converter;

import com.avalon.jpcap.skl.DTO.KeywordsDTO;
import com.avalon.jpcap.skl.vo.KeywordsVO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KeywordsVOConverter {
    /**
     * 将 KeywordsDTO 转换为 KeywordsVO
     *
     * @param dto KeywordsDTO 对象
     * @return KeywordsVO 对象
     */
    public KeywordsVO convertToVO(KeywordsDTO dto) {
        if (dto == null) {
            return null;
        }

        KeywordsVO vo = new KeywordsVO();
        vo.setKeyword(dto.getKeyword());
        vo.setCount(dto.getCount());

        return vo;
    }

    /**
     * 将多个 KeywordsDTO 转换为 KeywordsVO 对象列表
     *
     * @param dtos KeywordsDTO 对象列表
     * @return KeywordsVO 对象列表
     */
    public List<KeywordsVO> convertToVOList(List<KeywordsDTO> dtos) {
        return dtos.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
}
