package com.avalon.jpcap.infrastructure.mapper;

import com.avalon.jpcap.repository.po.OrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-07-04 18:14
 **/
@Mapper
public interface OrderRecordMapper {

    /**
     * insert
     * 返回主键
     */
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long insert(OrderPO po);

    /**
     * 通过主键id查询
     */
    OrderPO queryById(@Param("id") Long id);

    /**
     * 通过用户id和其他条件查询列表数据
     */
    List<OrderPO> queryByUserIdAndCondition(@Param("userId") Long userId,
                                            @Param("id") Long id,
                                            @Param("finished") Boolean finished,
                                            @Param("status") Integer status);

    /**
     * 更新数据（必须userId和id同时正确）
     */
    Boolean updateByUserIdAndId(OrderPO po);
}