package com.avalon.jpcap.security.service.impl;

import com.avalon.jpcap.repository.po.UserInfoPO;
import com.avalon.jpcap.repository.service.XlUserRepository;
import com.avalon.jpcap.security.converter.UserInfoConverter;
import com.avalon.jpcap.security.domain.dto.UserInfoDTO;
import com.avalon.jpcap.security.service.XlUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户security服务
 * @author DingHaoLun
 * @since 2022-10-31 18:07
 **/
@Service
@Slf4j
public class XlUserServiceImpl implements XlUserService {

    @Resource
    private XlUserRepository xlUserRepository;

    @Override
    public Long register(UserInfoDTO user) {
        return xlUserRepository.register(UserInfoConverter.dto2Po(user));
    }

    @Override
    public Boolean update(UserInfoDTO user) {
        return xlUserRepository.update(UserInfoConverter.dto2Po(user));
    }

    @Override
    public UserInfoDTO selectUserById(Long userId) {
        UserInfoPO po = xlUserRepository.selectUserById(userId);
        return UserInfoConverter.po2Dto(po);
    }

    @Override
    public Long getUserIdByPlatformPin(Integer platform, String platformPin) {
        return xlUserRepository.getUserIdByPlatformPin(platform, platformPin);
    }

    @Override
    public UserInfoDTO getUserByPlatformPin(Integer platform, String platformPin) {

        UserInfoPO po = xlUserRepository.getUserByPlatformPin(platform, platformPin);
        if(po!=null){
            return UserInfoConverter.po2Dto(po);
        }
        return null;
    }

    @Override
    public Boolean canGetUserByPlatformPin(Integer platform, String platformPin) {
        return xlUserRepository.canGetUserByPlatformPin(platform, platformPin);
    }
}