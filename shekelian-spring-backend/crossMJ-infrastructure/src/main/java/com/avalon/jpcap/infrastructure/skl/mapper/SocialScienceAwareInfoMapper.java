package com.avalon.jpcap.infrastructure.skl.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 省社科课题获奖信息映射器接口
 */

@Mapper
public interface SocialScienceAwareInfoMapper {
    /**
     * 查询获奖课题的总数
     */
    // 这里不需要创建索引，因为创建表的时候会自动创建主键索引
    @Select("SELECT COUNT(id) FROM social_science_aware_info") // 'id' 是主键字段的名字
    Integer countSocialScienceAwareNums(); // 返回类型应为 Integer 或 Long 以适应可能的较大数值范围
}
