package com.avalon.jpcap.security.converter;
import java.util.Date;
import java.util.Map;

import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.repository.po.UserInfoPO;
import com.avalon.jpcap.security.domain.dto.UserInfoDTO;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * @author DingHaoLun
 * @since 2022-11-14 15:34
 **/
public class UserInfoConverter {

    public static UserInfoPO dto2Po(UserInfoDTO dto){
        UserInfoPO po = new UserInfoPO();
        po.setId(dto.getUserId());
        po.setPin(dto.getPin());
        po.setPlatform(dto.getPlatform());
        po.setPlatformPin(dto.getPlatformPin());
        po.setCreateTime(new Date());
        po.setModifiedTime(new Date());
        po.setNickName(dto.getNickName());
        po.setImgUrl(dto.getHeadImgUrl());
        po.setCreator("system");
        po.setModifier("system");
        po.setExtJson(JsonUtils.toJson(dto.getExt()));
        return po;
    }

    public static UserInfoDTO po2Dto(UserInfoPO po){
        UserInfoDTO dto = new UserInfoDTO();
        dto.setUserId(po.getId());
        dto.setPin(po.getPin());
        dto.setPlatform(po.getPlatform());
        dto.setPlatformPin(po.getPlatformPin());
        dto.setNickName(po.getNickName());
        dto.setHeadImgUrl(po.getImgUrl());
        dto.setExt(JsonUtils.fromJson(po.getExtJson(), new TypeReference<Map<String, String>>() {}));
        return dto;
    }
}