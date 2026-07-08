package com.avalon.jpcap.infrastructure.impl.repo;

import com.avalon.jpcap.infrastructure.mapper.OrderRecordMapper;
import com.avalon.jpcap.repository.po.OrderPO;
import com.avalon.jpcap.repository.po.OrderQueryPO;
import com.avalon.jpcap.repository.po.PagePO;
import com.avalon.jpcap.repository.service.RecordRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-07-04 18:33
 **/
@Repository
public class OrderRecordRepositoryImpl implements RecordRepository<OrderPO, OrderQueryPO> {

    @Resource
    private OrderRecordMapper mapper;

    @Override
    public Long addRecord(OrderPO orderPO) {
        return mapper.insert(orderPO);
    }

    @Override
    public Boolean updateRecord(OrderPO orderPO) {
        return mapper.updateByUserIdAndId(orderPO);
    }

    @Override
    public Boolean updateRecordList(List<OrderPO> orderPOS) {
        return null;
    }

    @Override
    public OrderPO queryRecordByCondition(OrderQueryPO orderQueryPO) {
        List<OrderPO> list = mapper.queryByUserIdAndCondition(orderQueryPO.getUserId(), orderQueryPO.getId(), null, null);
        if(CollectionUtils.isNotEmpty(list)){
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<OrderPO> queryRecordListByCondition(OrderQueryPO orderQueryPO) {
        return mapper.queryByUserIdAndCondition(orderQueryPO.getUserId(), orderQueryPO.getId(), orderQueryPO.getFinished(), orderQueryPO.getStatus());
    }

    @Override
    public PagePO<OrderPO> queryPageRecordByCondition(OrderQueryPO orderQueryPO) {
        return null;
    }

    @Override
    public List<OrderPO> queryRecordListByIds(List<Long> Id) {
        return null;
    }
}