package com.avalon.jpcap.infrastructure.skl.mapper;

import com.avalon.jpcap.repository.skl.OrganizationCountPO;
import com.avalon.jpcap.repository.skl.OrganizationPO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrganizationMapper {

    /*
    建立索引：CREATE INDEX idx_id ON organization_info(id);
    这里依据id返回unit_name，不涉及复杂操作，因此只为id建立索引即可
     */
    @MapKey("id")  // 指定用 org_id 作为 Map 的 key
    @Select({
            "<script>",
            "SELECT id, unit_name FROM organization_info",
            "WHERE id IN",
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>",
            "   #{id}",
            "</foreach>",
            "</script>"
    })
    Map<Integer, OrganizationPO> selectUnit_NameByIDS(@Param("ids") List<Integer> ids);

    @Select("SELECT id as id BY name FROM organization_info")
    Integer selectIdByName(String name);
}
