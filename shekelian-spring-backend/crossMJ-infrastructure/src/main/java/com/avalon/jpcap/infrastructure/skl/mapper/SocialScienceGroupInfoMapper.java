package com.avalon.jpcap.infrastructure.skl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 四川社科团队信息映射器接口
 */

@Mapper
public interface SocialScienceGroupInfoMapper {

    /**
     * 查询社科团队的总数
     */
    // 这里不需要创建索引，因为创建表的时候会自动创建主键索引
    @Select("SELECT COUNT(id) FROM social_science_group_info")
    Integer shekeTeamsNums();
}
