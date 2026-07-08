package com.avalon.jpcap.infrastructure.skl.mapper;


import com.avalon.jpcap.repository.skl.AuthorCountPO;
import com.avalon.jpcap.repository.skl.CSCIPO;
import com.avalon.jpcap.repository.skl.TotalNumsCountPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * SSCI和HCI论文信息映射器接口
 */

@Mapper
public interface SSCIPaperInfoMapper {
    /**
     * 查询SSCI和HCI论文总数
     */
    @Select("SELECT COUNT(id) as count FROM ssci_and_hci_info")
    TotalNumsCountPO ssciTotalNums();

    /**
     * 查询SSCI和HCI论文出现次数前五的first_author_id及其计数
     */
    // 基本CRUD
    @Insert("INSERT INTO cssci_info(paper_title, first_author_id, publish_date) " +
                "VALUES(#{paperTitle}, #{firstAuthorId}, #{publishDate})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(CSCIPO CSCIPO);

    @Select("SELECT * FROM cssci_info WHERE id = #{id}")
    CSCIPO selectById(Integer id);

    // 查询指定作者的论文
    @Select("SELECT * FROM cssci_info WHERE first_author_id = #{authorId}")
        List<CSCIPO> selectByAuthorId(Integer authorId);

    // 获取前五作者ID及其论文数（返回PO是为了保持Mapper层纯洁性）
    @Select("SELECT first_author_id as id, COUNT(*) as paperCount " +
            "FROM cssci_info " +
            "GROUP BY first_author_id " +
            "ORDER BY paperCount DESC " +
            "LIMIT 5")
    List<AuthorCountPO> findTop5AuthorCounts();

}