package com.avalon.jpcap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

/**
 * @author DingHaoLun
 * @since 2022-11-12 18:18
 **/
@SpringBootApplication
@EnableCaching // 启用缓存
@ComponentScan("com.avalon")
@ImportResource(locations = {"classpath*:config.xml"})
public class App extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(App.class);
    }

    /**
     * 应用启动
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}