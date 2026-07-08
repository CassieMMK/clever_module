package com.avalon.jpcap.infrastructure.impl.repo;

import com.avalon.jpcap.infrastructure.mapper.VisitorRecordMapper;
import com.avalon.jpcap.repository.po.PagePO;
import com.avalon.jpcap.repository.po.VisitorPO;
import com.avalon.jpcap.repository.po.VisitorQueryPO;
import com.avalon.jpcap.repository.service.RecordRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author DingHaoLun
 * @since 2022-12-15 14:37
 **/
@Repository
public class VisitorRecordRepositoryImpl implements RecordRepository<VisitorPO, VisitorQueryPO> {

    @Resource
    private VisitorRecordMapper baseMapper;

    @Override
    public Long addRecord(VisitorPO visitorPO) {
        VisitorQueryPO queryPO = new VisitorQueryPO();
        queryPO.setProvince(visitorPO.getProvince());
        queryPO.setVisitDayStart(visitorPO.getVisitDay());
        queryPO.setVisitDayEnd(visitorPO.getVisitDay());

        //TODO：先查后写可能会有并发脏读。  处理1：redis分布式锁    处理2：mysql语句select for update 给mysql加行锁或表锁（无索引时）
        //https://segmentfault.com/a/1190000023045909
        List<VisitorPO> visitorPOList = baseMapper.selectList(queryPO);
        if (CollectionUtils.isNotEmpty(visitorPOList) && visitorPOList.size()==1) {
            VisitorPO selectResult = visitorPOList.get(0);
            //访问值增大
            selectResult.setProvinceVisitCountEachDay(selectResult.getProvinceVisitCountEachDay() + visitorPO.getProvinceVisitCountEachDay());
            baseMapper.updateById(selectResult);
            return selectResult.getId();
        } else{
            //首次插入值，返回主键id
            return baseMapper.insert(visitorPO);
        }
    }

    @Override
    public Boolean updateRecord(VisitorPO visitorPO) {
        return null;
    }

    @Override
    public Boolean updateRecordList(List<VisitorPO> visitorPOS) {
        return null;
    }

    @Override
    public VisitorPO queryRecordByCondition(VisitorQueryPO visitorQueryPO) {
        return null;
    }

    @Override
    public List<VisitorPO> queryRecordListByCondition(VisitorQueryPO queryPO) {
        List<VisitorPO> visitorPOResultList =  baseMapper.selectList(queryPO);
        return visitorPOResultList;
    }

    @Override
    public PagePO<VisitorPO> queryPageRecordByCondition(VisitorQueryPO queryPO) {
        return null;
    }

    @Override
    public List<VisitorPO> queryRecordListByIds(List<Long> Id) {
        return null;
    }
}