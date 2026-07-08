package com.avalon.jpcap.service;

import java.util.List;

/**
 * 元数据记录service,泛型。
 * @author: DingHaoLun
 * @create: 2022-12-15 14:32
 * T:元数据类型class   R：元数据查询条件class
 **/
public interface RecordService<T,R> {

    /**
     * 添加元数据记录
     */
    Long addRecord(T t);

    /**
     * 更新元数据记录
     */
    Boolean updateRecord(T t);

    /**
     * 通过条件查询单条记录
     */
    T queryRecordByCondition(R r);

    /**
     * 通过条件查询多条记录
     */
    List<T> queryRecordListByCondition(R r);
}
