package com.avalon.jpcap.security.filter;

import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.common.utils.SpringApplicationContextUtil;
import com.avalon.jpcap.security.service.TokenService;
import com.avalon.jpcap.security.service.XlUserService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * 用户除登录之外的请求，都要求必须携带JWT Token。所以我们需要一个Filter对这些请求做一个拦截。这个拦截器主要是提取header或cookie中的token，进行校验。
 * @author: DingHaoLun
 * @create: 2022-10-31 17:03
 **/
@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private TokenService tokenService;

    private XlUserService xlUserService;

    //非@Bean方式将bean注入到非IOC管理的类中。
    public JwtAuthenticationTokenFilter(){
        tokenService = SpringApplicationContextUtil.getBean(TokenService.class);
        xlUserService = SpringApplicationContextUtil.getBean(XlUserService.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
//        try{
//            //从cookie中获取token信息
//            Claims claims = tokenService.parseJwtCookie(request);
//            String platformPin = claims.getId();
//            String platform = claims.getIssuer();
//            Date now = new Date();
//
//            //查询缓存或mysql，是否存在此用户（这里查询的时候只通过openId查则会有多个不同应用下，用户是同一个unionId的情况）
//            Long userId = xlUserService.getUserIdByPlatformPin(Integer.parseInt(platform), platformPin);
//            if(now.before(claims.getExpiration()) && !Long.valueOf("-1").equals(userId)){
//
//                //1、通过登录校验，则将登录态放进 登录安全上下文中（TODO：此步骤必须有，否则将被jwt默认的几个filter返回403不通过，但是现在没搞清楚为什么）
//                //获取安全上下文对象，就是那个保存在 ThreadLocal 里面的安全上下文对象,其总是不为null否则403(如果不存在，则创建一个authentication属性为null的empty安全上下文对象)
//                SecurityContext securityContext  = SecurityContextHolder.getContext();
//                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(null,
//                        null, null);
//                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(
//                        request));
//                log.info("authentiion：{} ,setting security context", JsonUtils.toJson(authentication));
//                securityContext.setAuthentication(authentication);
//
//                //1、既然通过校验，就将值保存到request，方便保存到LoginContext上下文中取用
//                request.setAttribute("platform", Integer.parseInt(platform));
//                request.setAttribute("platformPin", platformPin);
//                request.setAttribute("userId", userId);
//                //2、如果没有过期，则继续续费cookie增加时间（TODO：暂时不做）
//                //3、调用链中的下一个筛选器，或者如果调用筛选器是链中的最后一个筛选器，则调用链末端的资源 https://zhuanlan.zhihu.com/p/161740475
//                filterChain.doFilter(request, response);
//            } else {
//                //否则要么没注册过、要么过期了，跳转到登录页。
//                response.setCharacterEncoding("UTF-8");
//                response.setContentType("application/json");
//                response.setStatus(401);//未登录
//                //当不调用下一个过滤器时（doFilter），将直接返回response
//            }
//        } catch (BusinessException e){
//            log.error("",e);
//            //否则要么没注册过、要么过期了，跳转到登录页。
//            response.setCharacterEncoding("UTF-8");
//            response.setContentType("application/json");
//            response.setStatus(401);//未登录
//            //当不调用下一个过滤器时（doFilter），将直接返回response
//        } catch (Exception e){
//            //否则要么没注册过、要么过期了，跳转到登录页。
//            log.error("",e);
//            response.setCharacterEncoding("UTF-8");
//            response.setContentType("application/json");
//            response.setStatus(401);//未登录
//            //当不调用下一个过滤器时（doFilter），将直接返回response
//        }


        //TODO:为了让学生能通过，直接默认写死用户是1
        Long userId = 1L;
        //1、既然通过校验，就将值保存到request，方便保存到LoginContext上下文中取用
        request.setAttribute("platform", null);
        request.setAttribute("platformPin", null);
        request.setAttribute("userId", userId);

        //1、通过登录校验，则将登录态放进 登录安全上下文中（TODO：此步骤必须有，否则将被jwt默认的几个filter返回403不通过，但是现在没搞清楚为什么）
        //获取安全上下文对象，就是那个保存在 ThreadLocal 里面的安全上下文对象,其总是不为null否则403(如果不存在，则创建一个authentication属性为null的empty安全上下文对象)
        SecurityContext securityContext  = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(null,
                null, null);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(
                request));
        log.info("authentiion：{} ,setting security context", JsonUtils.toJson(authentication));
        securityContext.setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}