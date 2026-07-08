package com.avalon.jpcap.service;

import com.avalon.jpcap.converter.OrderConverter;
import com.avalon.jpcap.domain.OrderDto;
import com.avalon.jpcap.domain.OrderQueryDto;
import com.avalon.jpcap.repository.po.OrderPO;
import com.avalon.jpcap.repository.po.OrderQueryPO;
import com.avalon.jpcap.repository.service.RecordRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 订单服务
 *
 * @author DingHaoLun
 * @since 2023-07-04 18:51
 **/
@Service
public class OrderService {

    @Resource(name = "orderRecordRepositoryImpl")
    private RecordRepository<OrderPO, OrderQueryPO> orderRecordRepository;

    /**
     * 生成未支付订单草稿
     */
    public Long createOrderId(OrderDto orderDto) {
        return orderRecordRepository.addRecord(OrderConverter.dto2Po(orderDto));
    }

    /**
     * 查询订单信息
     */
    public OrderDto queryOrder(OrderQueryDto queryDto){
        OrderPO po = orderRecordRepository.queryRecordByCondition(OrderConverter.dto2QueryPo(queryDto));
        return OrderConverter.po2Dto(po);
    }

    /**
     * 修改订单状态（支付订单）
     */
    public void payOrder(OrderDto orderDto){
        //userId和主键id确认无误后即可
        orderRecordRepository.updateRecord(OrderConverter.payDto2Po(orderDto));
    }


}