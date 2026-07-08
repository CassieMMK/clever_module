package com.avalon.jpcap.infrastructure.skl.mapper;

import com.avalon.jpcap.repository.skl.AuthorCountPO;
import com.avalon.jpcap.repository.skl.CSCIPO;
import com.avalon.jpcap.repository.skl.TotalNumsCountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.StatementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * CSCI论文信息映射器接口
 */
@Mapper
public interface CSCIMapper {
    /**
     * 查询CSCI论文总数
     */
    @Select("SELECT COUNT(id) as count FROM cssci_info")
    TotalNumsCountPO csciTotalNums();

    @Select("SELECT " +
            "id, " +
            "NULLIF(SUBSTRING_INDEX(author_list, ';', 1), '') AS firstAuthorName, " +
            "COALESCE(organization, '') AS organization, " +
            "COALESCE(author_list, '') AS authorList, " +
            "year, " +
            "COALESCE(research_direct, '') AS researchDirect, " +
            "COALESCE(obj_name, '') AS objName " +
            "FROM cssci_info " +
            "WHERE first_author_id = #{authorId}")
    List<CSCIPO> selectById(@Param("authorId") Integer authorId);

    @Select("SELECT " +
            "id, " +
            "NULLIF(SUBSTRING_INDEX(author_list, ';', 1), '') AS firstAuthorName, " +
            "COALESCE(author_list, '') AS authorList, " +
            "year, " +
            "COALESCE(research_direct, '') AS researchDirect, " +
            "COALESCE(obj_name, '') AS objName " +
            "FROM cssci_info " +
            "WHERE organization = #{organization}")
    List<CSCIPO> selectByOrg(String organization);


    @Select({
            "<script>",
            "SELECT ",
            "id, ",
            "NULLIF(SUBSTRING_INDEX(author_list, ';', 1), '') AS firstAuthorName, ",
            "COALESCE(organization, '') AS organization, ",
            "COALESCE(author_list, '') AS authorList, ",
            "year, ",
            "COALESCE(research_direct, '') AS researchDirect, ",
            "COALESCE(obj_name, '') AS objName ",
            "FROM cssci_info ",
            "WHERE 1=1",
            "<if test='paperName != null and paperName != \"\"'>",
            "   AND obj_name LIKE CONCAT('%', #{paperName}, '%')",
            "</if>",
            "<if test='Author != null and Author.trim() != \"\" '>",
            "   AND author_list REGEXP CONCAT('(^|;)', #{Author}, '($|;)')",
            "</if>",
            "<if test='organization != null and organization != \"\"'>",
            "   AND organization LIKE CONCAT('%', #{organization}, '%')",
            "</if>",
            "<if test='publishYear != null'>",
            "   AND year = #{publishYear}",
            "</if>",
            "<if test='researchField != null and researchField != \"\"'>",
            "   AND research_direct LIKE CONCAT('%', #{researchField}, '%')",
            "</if>",
            "</script>"
    })
    List<CSCIPO> selectCSCIArticles(
            @Param("paperName") String paperName,
            @Param("Author") String Author,
            @Param("organization") String organization,
            @Param("publishYear") Integer publishYear,
            @Param("researchField") String researchField
    );

    // 查询指定作者的论文
    @Select("SELECT * FROM cssci_info WHERE first_author_id = #{authorId}")
    List<CSCIPO> selectByAuthorId(Integer authorId);

    /**
     * 获取前五作者ID及其论文数
     */
    @Select("SELECT first_author_id as id, COUNT(*) as paperCount " +
            "FROM cssci_info " +
            "GROUP BY first_author_id " +
            "ORDER BY paperCount DESC " +
            "LIMIT 5")
    List<AuthorCountPO> findTop5AuthorCounts();

//    /**
//     * 关键词统计
//     * 数据是文本形式，建立全文索引
//     * CREATE FULLTEXT INDEX idx_keywords_fulltext ON cssci_info(keywords);
//     */
//    // 查询所有论文的关键词列(分页查询)
//    @Select("SELECT keyword, COUNT(*) AS count " +
//            "FROM (" +
//            "    SELECT TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(keywords, ';', n), ';', -1)) AS keyword " +
//            "    FROM cssci_info " +
//            "    CROSS JOIN (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) AS numbers " +
//            "    WHERE n <= (LENGTH(keywords) - LENGTH(REPLACE(keywords, ';', '')) + 1) " +
//            "    AND TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(keywords, ';', n), ';', -1)) != '' " +
//            ") AS keyword_table " +
//            "GROUP BY keyword " +
//            "ORDER BY count DESC")
//    List<Map<String, Object>> findKeywordStatistics();

    /**
     * 关键词统计 (使用存储过程和 JSON_TABLE 进行优化)
     * 假设可能存在 FULLTEXT INDEX idx_keywords_fulltext ON cssci_info(keywords)，
     * 但此特定查询方法不直接使用全文索引进行拆分。
     * 全文索引主要用于 MATCH()...AGAINST() 类型的查询。
     */
    @Select("{CALL CalculateKeywordStatistics()}")
    @Options(statementType = StatementType.CALLABLE)
    // 调用存储过程时非常重要
    List<Map<String, Object>> findKeywordStatistics();

}






