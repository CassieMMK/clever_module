package com.avalon.jpcap.infrastructure.skl.mapper;

import com.avalon.jpcap.repository.skl.PersonPO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PersonMapper {
    @MapKey("id")  // 指定用 id 作为 Map 的 key
    @Select({
            "<script>",
            "SELECT id, name FROM paper_person_info",
            "WHERE id IN",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>",
            "   #{id}",
            "</foreach>",
            "</script>"
    })
    Map<Integer, PersonPO> selectNameByIds(@Param("ids") List<Integer> ids);

    @Select("SELECT name FROM paper_person_info WHERE id = #{name}")
    String selectNameById(@Param("name") Integer id);

    @Select("SELECT id FROM paper_person_info WHERE name = #{name}")
    Integer selectIdByName(@Param("name") String name);

}