package com.avalon.jpcap.security.handler;

import com.avalon.jpcap.common.enums.ResultCodeEnum;
import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.common.utils.JsonUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * 当用户没有访问权限时的处理器，用于返回JSON格式的处理结果（若配置了全局异常的需要注意，会导致该处理类失效。需要在全局异常类里面在单独处理AccessDeniedException异常 ）；
 * @author: DingHaoLun
 * @create: 2022-10-31 15:25
 **/
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().println(JsonUtils.toJson(new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(),ResultCodeEnum.FORBIDDEN.getMsg())));
        response.getWriter().flush();
    }
}