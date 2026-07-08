package com.avalon.jpcap.infrastructure.skl.mapper;

import com.avalon.jpcap.repository.skl.OrganizationCountPO;
import com.avalon.jpcap.repository.skl.TopicCountByYearPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 省社科课题信息映射器接口
 */

@Mapper
//代表由 MyBatis 管理
public interface ProvinceTopicInfoMapper {

    /**
     * 查询省课题的总数
     */
    // 这里不需要创建索引，因为创建表的时候会自动创建主键索引
    @Select("SELECT COUNT(id) FROM province_topic_info")
    // 'id' 是主键字段的名字
    Integer countProvinceTopics(); // 返回类型应为 Integer 或 Long 以适应可能的较大数值范围

    /*
     * 历年省课题统计，每年分别有多少个课题，返回一个列表
     */
    // 在 create_time 列上创建单列索引
    // CREATE INDEX idx_province_topic_info_create_time ON province_topic_info(create_time);
    @Select("SELECT create_time as year, COUNT(*) AS topicCount " +
            "FROM province_topic_info " +
            "GROUP BY create_time " +
            "ORDER BY create_time")
    List<TopicCountByYearPO> getTopicCountByYear();

    /**
     * 获取课题数量为前五的单位
     * 建立索引：CREATE INDEX idx_org_id ON skldata.province_topic_info (org_id);
     * 依据org_id来统计
     */
    // 获取前五机关ID及其课题数（返回PO是为了保持Mapper层纯洁性）
    @Select("SELECT org_id as id, COUNT(*) as topicCount " +
            "FROM province_topic_info " +
            "GROUP BY org_id " +
            "ORDER BY topicCount DESC " +
            "LIMIT 5")
    List<OrganizationCountPO> findTop5OrganizationCounts();


    /*
    @Select("SELECT COUNT(*) FROM province_topic_info")
    int countProvinceTopics();为什么是这样的写法？
    解耦：将 SQL 语句放在 Mapper 接口中，可以将数据库操作的细节与业务逻辑分开。这样可以更容易地管理和维护数据库相关的代码。
    复用：如果你的应用中有多个地方需要执行相同的数据库操作，你可以在 Mapper 接口中定义一次，然后在不同的地方复用它。
    清晰性：将 SQL 语句放在 Mapper 接口中可以使代码更加清晰，易于理解和维护。
    灵活性：如果你的数据库操作逻辑发生变化，你只需要更新 Mapper 接口中的 SQL 语句，而不需要更改业务逻辑代码。
    安全性：使用 MyBatis 的映射机制可以帮助防止 SQL 注入攻击，因为它会自动处理 SQL 参数的转义。
    总结来说，使用 MyBatis 的 Mapper 接口可以更好地组织和管理数据库操作，提高代码的可读性、可维护性和安全性。
     */

}
