package com.avalon.jpcap.infrastructure.mapper;

import com.avalon.jpcap.repository.po.UserInfoPO;
import com.avalon.jpcap.repository.po.UserQueryQueryPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import java.util.List;

/**
 * @project: crossMJ
 * @description: 用户信息mapper
 * @author: DingHaoLun
 * @create: 2022-10-03 16:57
 **/
@Mapper
public interface UserInfoMapper{

    /**
     * 通过id查询
     */
    UserInfoPO selectById(Long id);

    /**
     * 通过第三方平台和平台的pin查询
     */
    List<UserInfoPO> selectList(UserQueryQueryPO queryPO);

    /**
     * 更新用户信息
     */
    Boolean updateUser(UserInfoPO userInfoPO);

    /**
     * 插入
     */
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    Long insert(UserInfoPO userInfoPO);
}