package com.avalon.jpcap.infrastructure.impl.repo;

import com.avalon.jpcap.infrastructure.mapper.MjRecordImgMapper;
import com.avalon.jpcap.repository.po.MjRecordImgPO;
import com.avalon.jpcap.repository.po.MjRecordImgQueryPO;
import com.avalon.jpcap.repository.po.PagePO;
import com.avalon.jpcap.repository.service.RecordRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-04-20 20:30
 **/
@Repository
public class MjRecordImgRepositoryImpl implements RecordRepository<MjRecordImgPO, MjRecordImgQueryPO> {
    @Resource
    private MjRecordImgMapper mapper;

    @Override
    public Long addRecord(MjRecordImgPO po) {
        return mapper.insert(po);
    }

    @Override
    public Boolean updateRecord(MjRecordImgPO mjRecordImgPO) {
        return mapper.updateById(mjRecordImgPO);
    }

    @Override
    public Boolean updateRecordList(List<MjRecordImgPO> mjRecordImgPOS) {
        if(CollectionUtils.isNotEmpty(mjRecordImgPOS)){
            return mapper.updateByIdList(mjRecordImgPOS);
        }
        return true;
    }

    @Override
    public MjRecordImgPO queryRecordByCondition(MjRecordImgQueryPO queryPO) {
        if(queryPO!=null && queryPO.getId()!=null){
            return mapper.queryById(queryPO.getId());
        }
        return null;
    }

    @Override
    public List<MjRecordImgPO> queryRecordListByCondition(MjRecordImgQueryPO queryPO) {
        return mapper.queryByCondition(queryPO);
    }

    @Override
    public PagePO<MjRecordImgPO> queryPageRecordByCondition(MjRecordImgQueryPO queryPO) {
        return null;
    }

    @Override
    public List<MjRecordImgPO> queryRecordListByIds(List<Long> Id) {
        return null;
    }
}