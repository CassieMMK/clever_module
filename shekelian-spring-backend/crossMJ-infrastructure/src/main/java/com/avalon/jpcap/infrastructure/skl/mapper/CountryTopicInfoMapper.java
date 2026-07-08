package com.avalon.jpcap.infrastructure.skl.mapper;

import com.avalon.jpcap.repository.skl.OrganizationCountPO;
import com.avalon.jpcap.repository.skl.TopicCountByYearPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 国家社科课题信息映射器接口
 */

@Mapper
public interface CountryTopicInfoMapper {
    /**
    /*国家社科课题总数
     */
    // 这里不需要创建索引，因为创建表的时候会自动创建主键索引
    @Select("SELECT COUNT(id) FROM national_social_science_aware_info")
    Integer countCountryTopics();

    /**
     * 国社科课题统计
     */


    /**
     * 获取国社科课题数量为前五的单位
     * 建立索引：CREATE INDEX idx_org_id ON national_social_science_aware_info(org_id);
     * 因为是依据org_id来统计。
     */
    // 获取前五机关ID及其课题数（返回PO是为了保持Mapper层纯洁性）
    @Select("SELECT org_id as id, COUNT(*) as topicCount " +
            "FROM national_social_science_aware_info " +
            "GROUP BY org_id " +
            "ORDER BY topicCount DESC " +
            "LIMIT 5")
    List<OrganizationCountPO> findTop5OrganizationCounts();

    /**
     * 历年国社科奖统计
     */

    @Select("SELECT year AS year, COUNT(*) AS topicCount " +
            "FROM national_social_science_aware_info " +
            "GROUP BY year " +
            "ORDER BY year DESC")
    List<TopicCountByYearPO> countTopicsByYear();
}
