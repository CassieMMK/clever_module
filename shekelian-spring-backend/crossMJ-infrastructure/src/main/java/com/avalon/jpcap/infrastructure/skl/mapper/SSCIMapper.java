package com.avalon.jpcap.infrastructure.skl.mapper;

import com.avalon.jpcap.repository.skl.AuthorCountPO;
import com.avalon.jpcap.repository.skl.CSCIPO;
import com.avalon.jpcap.repository.skl.SSCIPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * SSCI论文信息映射器接口
 */
@Mapper
public interface SSCIMapper {
    /**
     * 查询SSCI论文总数
     */
    @Select("SELECT COUNT(id) FROM ssci_and_hci_info ")
    long SSCITotalNums();

    @Select("SELECT " +
            "id, " +
            "NULLIF(SUBSTRING_INDEX(author_list, ';', 1), '') AS firstAuthorName, " +
            "COALESCE(organization, '') AS organization, " +
            "COALESCE(author_list, '') AS authorList, " +
            "year, " +
            "COALESCE(research_areas, '') AS researchAreas, " +
            "COALESCE(obj_name, '') AS objName " +
            "FROM ssci_and_hci_info " +
            "WHERE first_author_id = #{authorId}")
    List<SSCIPO> selectById(@Param("authorId") Integer authorId);

    @Select("SELECT " +
            "id, " +
            "NULLIF(SUBSTRING_INDEX(author_list, ';', 1), '') AS firstAuthorName, " +
            "COALESCE(author_list, '') AS authorList, " +
            "year, " +
            "COALESCE(research_areas, '') AS researchAreas, " +
            "COALESCE(obj_name, '') AS objName " +
            "FROM ssci_and_hci_info " +
            "WHERE organization = #{organization}")
    List<SSCIPO> selectByOrg(String organization);

    @Select({
            "<script>",
            "SELECT ",
            "id, ",
            "NULLIF(SUBSTRING_INDEX(author_list, ';', 1), '') AS firstAuthorName, ",
            "COALESCE(organization, '') AS organization, ",
            "COALESCE(author_list, '') AS authorList, ",
            "year, ",
            "COALESCE(research_areas, '') AS researchAreas, ",
            "COALESCE(obj_name, '') AS objName ",
            "FROM ssci_and_hci_info ",
            "WHERE 1=1",
            "<if test='paperName != null and paperName != \"\"'>",
            "   AND obj_name LIKE CONCAT('%', #{paperName}, '%')",
            "</if>",
            "<if test='Author != null and Author.trim() !=\"\" '>",
            "   AND author_list REGEXP CONCAT('(^|;)', #{Author}, '($|;)')",
            "</if>",
            "<if test='organization != null and organization != \"\"'>",
            "   AND organization LIKE CONCAT('%', #{organization}, '%')",
            "</if>",
            "<if test='publishYear != null'>",
            "   AND year = #{publishYear}",
            "</if>",
            "<if test='researchField != null and researchField != \"\"'>",
            "   AND research_areas LIKE CONCAT('%', #{researchField}, '%')",
            "</if>",
            "</script>"
    })
    List<SSCIPO> selectSSCIArticles(
            @Param("paperName") String paperName,
            @Param("Author") String Author,
            @Param("organization") String organization,
            @Param("publishYear") Integer publishYear,
            @Param("researchField") String researchField
    );


    // 查询指定作者的论文
    @Select("SELECT * FROM ssci_and_hci_info WHERE first_author_id = #{authorId}")
    List<SSCIPO> selectByAuthorId(Integer authorId);

    // 获取前五作者ID及其论文数（返回PO是为了保持Mapper层纯洁性）
    @Select("SELECT first_author_id as id, COUNT(*) as paperCount " +
            "FROM ssci_and_hci_info " +
            "GROUP BY first_author_id " +
            "ORDER BY paperCount DESC " +
            "LIMIT 5")
    List<AuthorCountPO> findTop5AuthorCounts();
}




