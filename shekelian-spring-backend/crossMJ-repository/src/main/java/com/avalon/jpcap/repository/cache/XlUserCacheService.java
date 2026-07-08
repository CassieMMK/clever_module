package com.avalon.jpcap.repository.cache;

import org.apache.commons.lang3.StringUtils;

/**
 * @project: crossMJ
 * @description:
 * @author: DingHaoLun
 * @create: 2023-03-09 17:05
 **/
public interface XlUserCacheService {

    /**
     * 通过第三方平台+平台的pin 能否查询用户信息记录,有的话则返回主键
     */
    Long select(Integer platform, String platformPin);

    /**
     * 如果不存在则插入到缓存并返回true，
     * 如果存在则返回false
     */
    Boolean setNx(Integer platform, String platformPin, Long id);

    /**
     * 是否能查询到
     */
    Boolean validCanSelect(Long value);
}
