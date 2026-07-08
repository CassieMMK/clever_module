package com.avalon.jpcap.common.filter;

import com.avalon.jpcap.common.filter.spi.LoginContextPostProcessor;
import com.avalon.jpcap.common.filter.vo.LoginVO;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 将登录信息放入LoginContext线程安全上下文中
 * @author DingHaoLun
 * @since 2022-11-16 16:44
 **/
@Component
public class LoginContextPostProcessorImpl implements LoginContextPostProcessor {
    @Override
    public void afterLogin(HttpServletRequest request, LoginVO loginVO) {
        loginVO.setPlatform((Integer)request.getAttribute("platform"));
        loginVO.setPlatformPin((String)request.getAttribute("platformPin"));
        loginVO.setUserId((Long)request.getAttribute("userId"));
    }
}