package com.avalon.jpcap.infrastructure.mapper;

import com.avalon.jpcap.repository.po.MjRecordImgPO;
import com.avalon.jpcap.repository.po.MjRecordImgQueryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-04-20 20:32
 **/
@Mapper
public interface MjRecordImgMapper {

    /**
     * insert
     * 返回主键
     */
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long insert(MjRecordImgPO po);

    /**
     * 通过主键id查询
     */
    MjRecordImgPO queryById(@Param("id") Long id);

    /**
     * 通过综合条件查询
     */
    List<MjRecordImgPO> queryByCondition(MjRecordImgQueryPO po);

    /**
     * 更新
     */
    Boolean updateById(MjRecordImgPO po);

    /**
     * 批量更新
     */
    Boolean updateByIdList(List<MjRecordImgPO> pos);
}