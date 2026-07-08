//package com.avalon.oper;
//
//import com.avalon.jpcap.security.filter.JwtAuthenticationTokenFilter;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.builders.WebSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
///**
// * 管理端filter过滤器配置
// *
// * @author DingHaoLun
// * @since 2023-05-12 16:04
// **/
//@Configuration
//@EnableWebSecurity
//@EnableGlobalMethodSecurity(prePostEnabled = true)
//@Slf4j
//public class OperSpringSecurityConfig extends WebSecurityConfigurerAdapter {
//
//    public OperSpringSecurityConfig(){
//        log.info("加载[管理端]-->spring登录过滤器配置");
//    }
//
//    // https://blog.csdn.net/henry_yang2018/article/details/113826321 多个过滤链如何配置
//
//    @Override
//    public void configure(WebSecurity web) {
//        // 允许对于网站静态资源的无授权访问
//        web.ignoring()
//                .antMatchers(HttpMethod.GET,
//                        "/",
//                        "/*.html",
//                        "/favicon.ico",
//                        "/**/*.html",
//                        "/**/*.css",
//                        "/**/*.js",
//                        "/*.ico");
//    }
//
//    @Override
//    protected void configure(HttpSecurity httpSecurity) throws Exception {
//        httpSecurity
//                .csrf().disable()// 由于使用的是JWT，我们这里不需要csrf校验
//                .sessionManagement()// 基于token令牌解密而非键对值，所以不需要session
//                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                .and()
//
//                //以下是鉴权url路径的配置规则
//                .authorizeRequests()
//                //跨域请求会先进行一次options请求
//                .antMatchers(HttpMethod.OPTIONS)
//                .permitAll()//以上的接口都无需鉴权
//                // 所有的oper路径的接口都要鉴权
//                .antMatchers(HttpMethod.GET, "/oper/**")
//                .authenticated()//其他请求全部需要鉴权认证
//                .antMatchers(HttpMethod.POST, "/oper/**")
//                .authenticated();
//
//        // 禁用缓存
//        httpSecurity.headers().cacheControl();
//        // 添加JWT filter，此方法是在args2过滤器之前，添加一个args1过滤器
//        httpSecurity.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
//
//    }
//
//    /**
//     * 我们自定义的JWT filter，不能用@Bean装载进来，否则就作为了bean自定义过滤器交给了spring IOC管理。
//     * 而filter是在servelet层，配置的antMatchers()规则对bean不生效，在spring Security的config无论怎么配都会触发这个过滤器。
//     * 因此不能用@Bean、@Component等bean的方式来初始化
//     */
//    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(){
//        return new JwtAuthenticationTokenFilter();
//    }
//}