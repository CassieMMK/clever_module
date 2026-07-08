package com.avalon.jpcap.converter;

import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.domain.OrderDto;
import com.avalon.jpcap.domain.OrderQueryDto;
import com.avalon.jpcap.repository.po.OrderPO;
import com.avalon.jpcap.repository.po.OrderQueryPO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;

/**
 * @author DingHaoLun
 * @since 2023-07-17 16:08
 **/
public class OrderConverter {

    public static OrderPO dto2Po(OrderDto dto){
        if(dto==null){
            return null;
        }
        OrderPO po = new OrderPO();
        po.setId(null);
        po.setUserId(dto.getUserId());
        po.setChannel(null);
        po.setFinished(null);
        po.setStatus(dto.getStatus());
        po.setAmount(dto.getAmount());
        po.setRefund(null);
        po.setExtJson(JsonUtils.toJson(dto.getExt()));
        po.setCreateTime(null);
        po.setModifiedTime(null);
        po.setCreator(null);
        po.setModifier(null);
        po.setYn(null);
        return po;
    }

    public static OrderDto po2Dto(OrderPO po){
        if(po == null){
            return null;
        }
        OrderDto dto = new OrderDto();
        dto.setId(po.getId());
        dto.setUserId(po.getUserId());
        dto.setAmount(po.getAmount());
        dto.setCreateTime(po.getCreateTime());
        dto.setPayTime(po.getPayTime());
        dto.setStatus(po.getStatus());
        dto.setChannel(po.getChannel());
        dto.setExt(JsonUtils.fromJson(po.getExtJson(), new TypeReference<Map<String, String>>() {}));
        return dto;
    }

    public static OrderQueryPO dto2QueryPo(OrderQueryDto dto){
        if(dto == null){
            return null;
        }
        OrderQueryPO po = new OrderQueryPO();
        po.setId(dto.getId());
        po.setUserId(dto.getUserId());
        po.setFinished(null);
        po.setStatus(dto.getStatus());
        return po;
    }

    /**
     * 支付dto转po
     */
    public static OrderPO payDto2Po(OrderDto dto){
        OrderPO po = new OrderPO();
        po.setId(dto.getId());
        po.setUserId(dto.getUserId());
        po.setChannel(dto.getChannel());
        po.setAmount(dto.getAmount());
        po.setPayTime(dto.getPayTime());
        po.setCreator("userId="+dto.getUserId().toString());
        po.setModifier("userId="+dto.getUserId().toString());
        return po;
    }
}