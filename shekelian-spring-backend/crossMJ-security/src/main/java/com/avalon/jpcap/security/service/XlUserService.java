package com.avalon.jpcap.security.service;
import com.avalon.jpcap.security.domain.dto.UserInfoDTO;

/**
 * @project: crossMJ
 * @description: 用户security的服务
 * @author: DingHaoLun
 * @create: 2022-10-31 17:51
 **/
public interface XlUserService {

    /**
     * 第三方平台pin的写入到我们应用数据库保存
     * 若已经存在，则不插入，直接返回主键Id
     * @param user
     * @return boolean
     **/
    Long register(UserInfoDTO user);

    /**
     * 修改用户
     * @param user
     * @return boolean
     **/
    Boolean update(UserInfoDTO user);

    /**
     * 通过用户ID查询用户
     * @param userId 用户ID
     * @return 用户对象信息
     */
    UserInfoDTO selectUserById(Long userId);

    /**
     * 通过第三方平台的pin查询用户id
     */
    Long getUserIdByPlatformPin(Integer platform, String platformPin);

    /**
     * 通过第三方平台的pin查询用户
     */
    UserInfoDTO getUserByPlatformPin(Integer platform, String platformPin);

    /**
     * 是否可以查询到用户
     */
    Boolean canGetUserByPlatformPin(Integer platform, String platformPin);
}