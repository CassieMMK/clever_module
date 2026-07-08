package com.avalon.jpcap.infrastructure.mapper;

import com.avalon.jpcap.repository.po.VisitorPO;
import com.avalon.jpcap.repository.po.VisitorQueryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2022-12-15 18:29
 **/
@Mapper
public interface VisitorRecordMapper {

    /**
     * 插入
     */
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long insert(VisitorPO po);

    /**
     * 列表查询
     */
    List<VisitorPO> selectList(VisitorQueryPO queryPO);

    Boolean selectById(Long id);

    Boolean updateById(VisitorPO po);
}