package com.avalon.jpcap.security.service;

import com.avalon.jpcap.security.domain.vo.TokenHeaderVO;
import io.jsonwebtoken.Claims;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * jwt token生成和解析服务
 * @author DingHaoLun
 * @since 2022-11-08 16:41
 **/
public interface TokenService {

    /**
     * 注册cookie，生成jwt
     */
    TokenHeaderVO createJwt(Map<String,Object> claims);

    /**
     * 获取header中保存的token
     * 解析token中的信息，用于校验登录态
     */
    Claims parseJwt(HttpServletRequest httpServletRequest);

    /**
     * 从cookie中获取token信息，用于校验登录态
     */
    Claims parseJwtCookie(HttpServletRequest httpServletRequest);
}
