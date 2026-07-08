package com.avalon.jpcap.converter;

import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.domain.PromptModelDto;
import com.avalon.jpcap.domain.PromptModelQueryDto;
import com.avalon.jpcap.repository.po.PromptModelPO;
import com.avalon.jpcap.repository.po.PromptModelQueryPO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * @author DingHaoLun
 * @since 2023-05-15 14:02
 **/
public class MjPromptModelConverter {

    public static PromptModelPO dto2Po(PromptModelDto dto){

        PromptModelPO po = new PromptModelPO();
        po.setId(dto.getPromptModelId());
        po.setUserId(dto.getUserId());
        po.setPrompt(dto.getPrompt());
        po.setModelImgUrl(dto.getModelImgUrl());
        po.setExtJson(dto.getExt().toString());
        po.setCreator("system");
        po.setModifier("system");
        return po;
    }

    public static PromptModelDto po2Dto(PromptModelPO po){
        PromptModelDto dto = new PromptModelDto();
        dto.setUserId(po.getUserId());
        dto.setPrompt(po.getPrompt());
        dto.setUsedTimes(po.getUsedTimes());
        dto.setLikeTimes(po.getLikeTimes());
        dto.setModelImgUrl(po.getModelImgUrl());
        dto.setExt(po.getExtJson());
        return dto;
    }

    public static PromptModelQueryPO dto2Po(PromptModelQueryDto dto){
        PromptModelQueryPO queryPO = new PromptModelQueryPO();
        queryPO.setId(dto.getId());
        queryPO.setUserId(dto.getUserId());
        queryPO.setOrderByUsedTimesDesc(dto.getOrderByUsedTimesDesc());
        queryPO.setOrderByLikeTimesDesc(dto.getOrderByLikeTimesDesc());
        queryPO.setCurrentPage(dto.getCurrentPage());
        queryPO.setPageSize(dto.getPageSize());

        return queryPO;
    }
}