package com.avalon.jpcap.infrastructure.impl.repo;

import com.avalon.jpcap.repository.cache.XlUserCacheService;
import com.avalon.jpcap.infrastructure.mapper.UserInfoMapper;
import com.avalon.jpcap.repository.po.PagePO;
import com.avalon.jpcap.repository.po.UserInfoPO;
import com.avalon.jpcap.repository.po.UserQueryQueryPO;
import com.avalon.jpcap.repository.service.XlUserRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author DingHaoLun
 * @since 2022-11-12 16:20
 **/
@Repository
public class XlUserRepositoryImpl implements XlUserRepository {

    @Resource
    private XlUserCacheService cacheService;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Override
    public Long register(UserInfoPO user) {
        UserInfoPO newUserInfoPO = new UserInfoPO();
        BeanUtils.copyProperties(user, newUserInfoPO);
        //先校验是否已经注册过了,先缓存校验
        Long selectResult = cacheService.select(user.getPlatform(), user.getPlatformPin());
        if(cacheService.validCanSelect(selectResult)){
            return selectResult;
        }
        //TODO：缓存查询不到则查询数据库,先查后写，这里有并发风险
        UserQueryQueryPO userQueryPO = new UserQueryQueryPO();
        userQueryPO.setPlatform(user.getPlatform());
        userQueryPO.setPlatformPin(user.getPlatformPin());

        List<UserInfoPO> userInfoPOS = userInfoMapper.selectList(userQueryPO);
        if (CollectionUtils.isNotEmpty(userInfoPOS)) {
            return userInfoPOS.get(0).getId();
        }
        //都查不到说明是新用户，插入一个新的，并写入redis缓存
        Long userId = userInfoMapper.insert(newUserInfoPO);
        cacheService.setNx(user.getPlatform(), user.getPlatformPin(), userId);
        return userId;
    }

    @Override
    public Boolean update(UserInfoPO user) {
        return false;
    }

    @Override
    public UserInfoPO getUserByPlatformPin(Integer platform, String platformPin) {
        UserQueryQueryPO userQueryPO = new UserQueryQueryPO();
        userQueryPO.setPlatform(platform);
        userQueryPO.setPlatformPin(platformPin);

        List<UserInfoPO> userInfoPOS = userInfoMapper.selectList(userQueryPO);
        if(CollectionUtils.isNotEmpty(userInfoPOS) && userInfoPOS.size()==1){
            return userInfoPOS.get(0);
        }
        return null;
    }

    @Override
    public Long getUserIdByPlatformPin(Integer platform, String platformPin){
        //1、先尝试从缓存中获取，缓存中没有话再尝试从数据库捞数据（缓存可以是本地map，也可以是redis）
        Long selectResult = cacheService.select(platform, platformPin);
        if(cacheService.validCanSelect(selectResult)){
            return selectResult;
        }
        //2、缓存中拿不到，去数据库查询
        UserQueryQueryPO userQueryPO = new UserQueryQueryPO();
        userQueryPO.setPlatform(platform);
        userQueryPO.setPlatformPin(platformPin);
        List<UserInfoPO> userInfoPOS = userInfoMapper.selectList(userQueryPO);
        if(CollectionUtils.isNotEmpty(userInfoPOS) && userInfoPOS.size()==1){
            //能查到，放入缓存中，加快下次查询效率
            UserInfoPO po = userInfoPOS.get(0);
            cacheService.setNx(platform, platformPin, po.getId());
            return po.getId();
        } else {
            return -1L;
        }
    }

    @Override
    public Boolean canGetUserByPlatformPin(Integer platform, String platformPin) {
        //1、先尝试从缓存中获取，缓存中没有话再尝试从数据库捞数据（缓存可以是本地map，也可以是redis）
        Long selectResult = cacheService.select(platform, platformPin);
        if(cacheService.validCanSelect(selectResult)){
            return Boolean.TRUE;
        }

        //2、缓存中拿不到，去数据库查询
        UserQueryQueryPO userQueryPO = new UserQueryQueryPO();
        userQueryPO.setPlatform(platform);
        userQueryPO.setPlatformPin(platformPin);
        List<UserInfoPO> userInfoPOS = userInfoMapper.selectList(userQueryPO);
        if(CollectionUtils.isNotEmpty(userInfoPOS)&&userInfoPOS.size()==1){
            //能查到，放入缓存中，加快下次查询效率
            UserInfoPO po = userInfoPOS.get(0);
            cacheService.setNx(platform, platformPin, po.getId());
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    //mybatis的此处语法不好写，最好直接写数据库语句
    @Override
    public PagePO<UserInfoPO> listMemberPage(UserQueryQueryPO queryPO) {
        return null;
    }

    @Override
    public Boolean delete(List<Long> userIds) {
        return false;
    }

    @Override
    public UserInfoPO selectUserById(Long userId) {
        return userInfoMapper.selectById(userId);
    }
}