package com.avalon.jpcap.config;

import com.avalon.jpcap.security.filter.JwtAuthenticationTokenFilter;
import com.avalon.jpcap.security.handler.CustomAccessDeniedHandler;
import com.avalon.jpcap.security.handler.CustomAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;

/**
 * @project: crossMJ
 * @description: 配置类，配置springSecurity的验证规则
 * @author: DingHaoLun
 * @create: 2022-10-31 14:44
 **/
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Slf4j
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {
    @Resource
    private CustomAccessDeniedHandler customAccessDeniedHandler;
    @Resource
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    public SpringSecurityConfig(){
        log.info("加载[用户端]-->spring登录过滤器配置");
    }

    @Override
    public void configure(WebSecurity web) {
        //解决静态资源被拦截的问题
        web.ignoring()
                // 允许不登录匿名访问的，其他的都默认拦截
                .antMatchers("/avalon/login")
                .antMatchers("/avalon/needNotLogin");
                //TODO：暂时先放开actuator
                //.antMatchers("/actuator/*");

        // 允许对于网站静态资源的无授权访问,包括swagger knife4j
        web.ignoring()
                .antMatchers(HttpMethod.GET,
                "/",
                "/*.html",
                "/favicon.ico",
                "/**/*.html",
                "/**/*.css",
                "/**/*.js",
                "/swagger-resources/**", //swagger
                "/v3/api-docs",//knife4j
                "/v2/api-docs/**",
                "/*.ico");
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf().disable()// 由于使用的是JWT，我们这里不需要csrf校验
                .sessionManagement()// 基于token令牌解密而非键对值，所以不需要session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                //跨域请求会先进行一次options请求
                .antMatchers(HttpMethod.OPTIONS)
                .permitAll()
                .anyRequest()// 除上面外的所有请求外，
                .authenticated();//其他请求全部需要鉴权认证

        // 禁用缓存
        httpSecurity.headers().cacheControl();
        // 添加JWT filter，此方法是在args2过滤器之前，添加一个args1过滤器
        httpSecurity.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
        //TODO:这里似乎有问题，添加自定义未授权和未登录结果返回
        httpSecurity.exceptionHandling()
                .authenticationEntryPoint(customAuthenticationEntryPoint) //token无效的拒绝策略
                .accessDeniedHandler(customAccessDeniedHandler);//权限不足的拒绝策略

    }

    /**
     * 我们自定义的JWT filter，不能用@Bean装载进来，否则就作为了bean自定义过滤器交给了spring IOC管理。
     * 而filter是在servelet层，配置的antMatchers()规则对bean不生效，在spring Security的config无论怎么配都会触发这个过滤器。
     * 因此不能用@Bean、@Component等bean的方式来初始化
     */
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(){
        return new JwtAuthenticationTokenFilter();
    }
}