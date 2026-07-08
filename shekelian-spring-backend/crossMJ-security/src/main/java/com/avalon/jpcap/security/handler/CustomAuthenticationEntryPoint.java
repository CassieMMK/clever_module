package com.avalon.jpcap.security.handler;

import com.avalon.jpcap.common.enums.ResultCodeEnum;
import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.common.utils.JsonUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 当未登录或token失效时的处理器
 * @author: DingHaoLun
 * @create: 2022-10-31 15:33
 **/
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().println(JsonUtils.toJson(new BusinessException(ResultCodeEnum.UNAUTHORIZED.getCode(),ResultCodeEnum.UNAUTHORIZED.getMsg())));
        response.getWriter().flush();
    }
}