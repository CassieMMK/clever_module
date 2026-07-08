package com.avalon.jpcap.infrastructure.mapper;

import com.avalon.jpcap.repository.po.PromptModelPO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2023-05-13 20:11
 **/
@Mapper
public interface PromptModelMapper {
    /**
     * insert
     * 返回主键
     */
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long insert(PromptModelPO po);

    /**
     * 通过主键id查询
     */
    PromptModelPO queryById(@Param("id") Long id);

    /**
     * 根据使用量usedTimes或点赞量likeTimes排序查询
     */
    Page<PromptModelPO> queryPageByRank(@Param("userId") Long userId,
                                        @Param("usedTimesRank") Boolean usedTimesRank, @Param("likeTimesRank") Boolean likeTimesRank,
                                        @Param("pageNum") int pageNum,
                                        @Param("pageSize") int pageSize); //pageNum和pageSize是pageHelper插件的默认字段，不要修改

    /**
     * 更新
     */
    Boolean updateById(PromptModelPO po);
}