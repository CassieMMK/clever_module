package com.avalon.jpcap.infrastructure.impl.cache;

import com.avalon.jpcap.infrastructure.cache.RedisService;
import com.avalon.jpcap.repository.cache.XlUserCacheService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author DingHaoLun
 * @since 2022-11-14 16:11
 **/
@Service
public class XlUserCacheServiceImpl implements XlUserCacheService {

    @Resource
    private RedisService redisService;

    //默认30天过期
    @Value("${user.cache.expire:2592000}")
    private Long expireSecond;

    private static String PLATFORM_PREFIX = "PLATFORM";

    private static Long NOT_EXIT_VALUE = -1L;

    /**
     * 通过第三方平台+平台的pin 能否查询用户信息记录,有的话则返回主键
     */
    @Override
    public Long select(Integer platform, String platformPin){
       String value = (String) redisService.get(PLATFORM_PREFIX + "_" + platform.toString() + "_" + platformPin);
       if(StringUtils.isNotBlank(value)){
           return Long.parseLong(value);
       }
       return NOT_EXIT_VALUE;
    }

    @Override
    public Boolean setNx(Integer platform, String platformPin, Long id){
        return redisService.setNx(PLATFORM_PREFIX + "_" + platform.toString()+ "_" + platformPin, id.toString(), expireSecond);
    }




    /**
     * 是否能查询到
     */
    @Override
    public Boolean validCanSelect(Long value){
        if(NOT_EXIT_VALUE.equals(value) || value == null){
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }
}