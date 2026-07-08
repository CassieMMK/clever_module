package com.avalon.jpcap.infrastructure.mapper;

import com.avalon.jpcap.repository.po.MjMemberPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import java.util.List;

/**
 * MjMemberMapper
 *
 * @author DingHaoLun
 * @since 2023-04-17 16:16
 **/
@Mapper
public interface MjMemberMapper {

    /**
     * 插入，返回用户等级主键id
     */
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    public Long insert(MjMemberPO po);

    /**
     * 通过用户id查询
     */
    public List<MjMemberPO> queryByUserId(Long UserId);

    /**
     * 更新会员信息
     */
    public void update(MjMemberPO po);
}