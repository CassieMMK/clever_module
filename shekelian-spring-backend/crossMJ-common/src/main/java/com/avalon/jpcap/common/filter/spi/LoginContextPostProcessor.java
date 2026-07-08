package com.avalon.jpcap.common.filter.spi;

import com.avalon.jpcap.common.filter.vo.LoginVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @project: crossMJ
 * @description:
 * @author: DingHaoLun
 * @create: 2022-11-07 11:03
 **/
public interface LoginContextPostProcessor {
    /**
     * 登录后处理
     *
     * @param request 请求对象
     * @param loginVO 登录对象
     */
    void afterLogin(HttpServletRequest request, LoginVO loginVO);
}
