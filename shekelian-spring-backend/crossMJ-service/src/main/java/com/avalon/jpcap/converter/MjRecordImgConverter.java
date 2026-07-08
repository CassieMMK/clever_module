package com.avalon.jpcap.converter;

import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.domain.MjRecordImgDto;
import com.avalon.jpcap.repository.po.MjRecordImgPO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author DingHaoLun
 * @since 2023-04-20 20:52
 **/
public class MjRecordImgConverter {

    public static List<MjRecordImgDto> pos2Dtos(List<MjRecordImgPO> pos){
        if(CollectionUtils.isEmpty(pos)){
            return null;
        }
        List<MjRecordImgDto> dtos = pos.stream().map(po -> {
            MjRecordImgDto dto = new MjRecordImgDto();

            dto.setFatherImgRecordId(po.getFatherId());
            dto.setChildImgIndex(po.getChildImgIndex());

            dto.setImgRecordId(po.getId());
            dto.setUserId(po.getUserId());
            dto.setTime(po.getCreateTime());
            dto.setImgUrlOuter(po.getImgUrlOuter());
            dto.setImgUrl(po.getImgUrl());
            dto.setType(po.getType());
            dto.setUsed(po.getUsed());
            dto.setExt(po.getExtJson());
            return dto;
        }).collect(Collectors.toList());

        return dtos;
    }
}