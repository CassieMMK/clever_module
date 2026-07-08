package com.avalon.jpcap.converter;

import com.avalon.jpcap.common.enums.MjMemberActivateEnum;
import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.domain.MjMemberDto;
import com.avalon.jpcap.repository.po.MjMemberPO;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
*
*
* @author DingHaoLun
*
* @since 2023-04-17 17:11
**/
public class MjMemberConverter {

    public static List<MjMemberDto> pos2Dtos(List<MjMemberPO> pos){
        if(CollectionUtils.isEmpty(pos)){
            return null;
        }
        List<MjMemberDto> dtos = pos.stream().map(po -> {
            MjMemberDto dto = new MjMemberDto();
            dto.setUserId(po.getUserId());
            dto.setMemberLevel(po.getMemberLevel());
            if(MjMemberActivateEnum.VALID.getCode().equals(po.getIsActivate())){
                dto.setIsActivate(Boolean.TRUE);
            }else {
                dto.setIsActivate(Boolean.FALSE);
            }
            dto.setCredit(po.getCredit());
            dto.setExt(po.getExtJson());
            return dto;
        }).collect(Collectors.toList());
        return dtos;
    }

    public static MjMemberPO dto2Po(MjMemberDto dto){
        if(dto==null){
            return null;
        }
        MjMemberPO po = new MjMemberPO();
        po.setUserId(dto.getUserId());
        po.setMemberLevel(dto.getMemberLevel());
        po.setIsActivate(MjMemberActivateEnum.VALID.getCode());
        po.setCredit(dto.getCredit());
        po.setExtJson(dto.getExt().toString());
        return po;
    }


}