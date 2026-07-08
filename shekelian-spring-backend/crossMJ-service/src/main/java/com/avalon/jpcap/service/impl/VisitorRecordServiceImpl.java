package com.avalon.jpcap.service.impl;

import com.avalon.jpcap.converter.VisitorConverter;
import com.avalon.jpcap.domain.VisitorRecordDTO;
import com.avalon.jpcap.domain.VisitorRecordQueryDTO;
import com.avalon.jpcap.repository.po.VisitorPO;
import com.avalon.jpcap.repository.po.VisitorQueryPO;
import com.avalon.jpcap.repository.service.RecordRepository;
import com.avalon.jpcap.service.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户访问来源省份统计
 * @author DingHaoLun
 * @since 2022-12-15 14:34
 **/
@Service
public class VisitorRecordServiceImpl implements RecordService<VisitorRecordDTO, VisitorRecordQueryDTO> {

    @Autowired
    private RecordRepository<VisitorPO, VisitorQueryPO> recordRepository;

    @Override
    public Long addRecord(VisitorRecordDTO t) {
        return recordRepository.addRecord(VisitorConverter.dto2Po(t));
    }

    @Override
    public Boolean updateRecord(VisitorRecordDTO visitorRecordDTO) {
        return null;
    }

    /**此方法不提供*/
    @Override
    public VisitorRecordDTO queryRecordByCondition(VisitorRecordQueryDTO r) {
        return null;
    }

    @Override
    public List<VisitorRecordDTO> queryRecordListByCondition(VisitorRecordQueryDTO r) {
        List<VisitorPO> visitorPOList = recordRepository.queryRecordListByCondition(VisitorConverter.dto2Po(r));
        if(!CollectionUtils.isEmpty(visitorPOList)){
            return visitorPOList.stream().map(VisitorConverter::po2Dto).collect(Collectors.toList());
        }
        return null;
    }
}