package com.avalon.jpcap.converter;

import com.avalon.jpcap.domain.VisitorRecordDTO;
import com.avalon.jpcap.domain.VisitorRecordQueryDTO;
import com.avalon.jpcap.repository.po.VisitorPO;
import com.avalon.jpcap.repository.po.VisitorQueryPO;
import lombok.SneakyThrows;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author DingHaoLun
 * @since 2022-11-14 15:34
 **/
public class VisitorConverter {

    public static VisitorPO dto2Po(VisitorRecordDTO dto){
        VisitorPO po = new VisitorPO();
        po.setProvince(dto.getProvince());

        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");

        po.setVisitDay(simpleDateFormat.format(dto.getVisitDay()));
        po.setProvinceVisitCountEachDay(dto.getProvinceVisitCountEachDay());
        po.setCreateTime(new Date());
        po.setModifiedTime(new Date());
        po.setCreator("system");
        po.setModifier("system");
        return po;
    }

    @SneakyThrows
    public static VisitorRecordDTO po2Dto(VisitorPO po){
        VisitorRecordDTO dto = new VisitorRecordDTO();
        dto.setProvince(po.getProvince());
        dto.setProvinceVisitCountEachDay(po.getProvinceVisitCountEachDay());
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        dto.setVisitDay(simpleDateFormat.parse(po.getVisitDay()));
        return dto;
    }

    public static VisitorQueryPO dto2Po(VisitorRecordQueryDTO dto){
        VisitorQueryPO po = new VisitorQueryPO();
        po.setProvince(dto.getProvince());
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd");
        po.setVisitDayStart(simpleDateFormat.format(dto.getVisitDayStart()));
        po.setVisitDayEnd(simpleDateFormat.format(dto.getVisitDayEnd()));
        return po;
    }
}