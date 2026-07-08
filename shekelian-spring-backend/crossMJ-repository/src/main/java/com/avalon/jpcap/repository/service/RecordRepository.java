package com.avalon.jpcap.repository.service;

import com.avalon.jpcap.repository.po.PagePO;

import java.util.List;

/**
 * 元数据落库服务
 * @author : DingHaoLun
 * @since ： 2022-12-15 15:02
 **/
public interface RecordRepository<T,R>{

    /**
     * 添加元数据记录
     */
    Long addRecord(T t);

    /**
     * 更新元数据记录
     */
    Boolean updateRecord(T t);

    Boolean updateRecordList(List<T> tList);

    /**
     * 通过条件查询单条记录
     */
    T queryRecordByCondition(R r);

    /**
     * 通过条件查询多条记录
     */
    List<T> queryRecordListByCondition(R r);

    /**
     * 通过条件分页查询多条记录
     */
    PagePO<T> queryPageRecordByCondition(R r);

    /**
     * 通过某索引id批量查询多条记录
     */
    List<T> queryRecordListByIds(List<Long> Id);
}
