package com.avalon.jpcap.common.filter;

import com.avalon.jpcap.common.result.MDCHelper;
import com.avalon.jpcap.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * http访问进来时的拦截器，用于给请求初始化MDCHelper中所需的 operater、identity、traceId
 * @author: DingHaoLun
 * @create: 2022-09-28 17:25
 **/
@Slf4j
@Order(1)
@Component
public class TraceFilter implements HandlerInterceptor {
    /**
     * 定义KEY
     */
    private static final String PRE_HANDLE_TIME = "PRE_HANDLE_TIME";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute(PRE_HANDLE_TIME, System.currentTimeMillis());
        try {
            MDCHelper.install(null, LoginContextHelper.loadLoginInfo().getPin(),
                    LoginContextHelper.loadLoginInfo().getIdentity());
        } catch (Exception e) {
            log.error("mdc load pin/identity error", e);
            MDCHelper.install(null, "", "");
        }

        if (log.isInfoEnabled()) {
            log.info("【HTTP日志】 >>> - url:{}, method:{}, ip:{}, requestParams:{}, handlerDetails:{}, headers:{}, 登录态loginVO为{}",
                    request.getRequestURL().toString(),
                    request.getMethod(),
                    request.getRemoteAddr(),
                    loadRequestParams(request),
                    handler,
                    loadHeaders(request),
                    JsonUtils.toJson(LoginContextHelper.loadLoginInfo()));
        }

        return true;
    }

    private Map<String, String> loadRequestParams(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        for (Object key : request.getParameterMap().keySet()) {
            if (key instanceof String) {
                String value = request.getParameter((String) key);
                result.put((String) key, value);
            }
        }
        return result;
    }

    @Override
    public void afterCompletion(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nullable Object handler, Exception ex) throws Exception {
        Long tick = (Long) request.getAttribute(PRE_HANDLE_TIME);
        if (Objects.nonNull(tick) && log.isInfoEnabled()) {
            log.info("【HTTP日志】 <<< - usedTime: {}(秒), - traceId: {}", (System.currentTimeMillis() - tick) / 1000.0, MDCHelper.getTraceId());
        }
        MDCHelper.uninstall();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> loadHeaders(HttpServletRequest request) {
        Map<String, String> result = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            result.put(name, request.getHeader(name));
        }
        return result;
    }
}