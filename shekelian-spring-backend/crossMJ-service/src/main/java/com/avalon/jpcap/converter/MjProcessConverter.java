package com.avalon.jpcap.converter;

import com.avalon.jpcap.domain.MjProcessDto;
import com.avalon.jpcap.repository.po.MjProcessPO;

/**
 * @author DingHaoLun
 * @since 2023-04-19 16:10
 **/
public class MjProcessConverter {

    public static MjProcessDto po2Dto(MjProcessPO po){
        if(po==null){
            return null;
        }
        MjProcessDto dto = new MjProcessDto();
        dto.setUserId(po.getUserId());
        dto.setStreamKey(po.getStreamKey());
        dto.setMessageId(po.getMessageId());
        dto.setRank(po.getRank());
        dto.setType(po.getType());
        dto.setPrompt(po.getPrompt());
        return dto;
    }
}