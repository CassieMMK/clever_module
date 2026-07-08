package com.avalon.jpcap.repository.service;

import com.avalon.jpcap.repository.po.PagePO;
import com.avalon.jpcap.repository.po.UserInfoPO;
import com.avalon.jpcap.repository.po.UserQueryQueryPO;

import java.util.List;

/**
 * @author : DingHaoLun
 * @since : 2022-11-12 16:19
 **/
public interface XlUserRepository {


    /**
     * 第三方平台pin的写入到我们应用数据库保存
     * @param user
     * @return boolean
     **/
    Long register(UserInfoPO user);

    /**
     * 修改用户
     * @param user
     * @return boolean
     **/
    Boolean update(UserInfoPO user);

    /**
     * 通过用户ID查询用户
     * @param userId 用户ID
     * @return 用户对象信息
     */
    UserInfoPO selectUserById(Long userId);

    /**
     * 通过第三方平台的pin查询用户(DB)
     */
    UserInfoPO getUserByPlatformPin(Integer platform, String platformPin);

    /**
     * 通过第三方平台pin查询用户id（Cache or DB）
     * @return 用户id，查询不到则为-1
     */
    Long getUserIdByPlatformPin(Integer platform, String platformPin);

    /**
     * 通过第三方平台的pin是否能查询到用户
     */
    Boolean canGetUserByPlatformPin(Integer platform, String platformPin);

    /**
     * 获取列表。分页
     */
    PagePO<UserInfoPO> listMemberPage(UserQueryQueryPO queryPO);

    /**
     * 批量删除用户
     * @param userIds
     * @return boolean
     **/
    Boolean delete(List<Long> userIds);
}
