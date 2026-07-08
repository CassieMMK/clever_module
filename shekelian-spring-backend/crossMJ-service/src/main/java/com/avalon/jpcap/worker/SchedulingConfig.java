package com.avalon.jpcap.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author dinghaolun
 */
@Configuration
@ConditionalOnProperty(prefix = "scheduling", name = "enabled", havingValue = "true") //当properties配置中scheduling.enabled=true时才启用下面的定时任务配置
@EnableScheduling //设置开启@Schedule注解修饰的定时任务
@Slf4j
public class SchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        //10线程worker，一个worker处理慢，到了下一次定时时间，会有另一个线程继续执行，无需阻塞等待上一个结束
        ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(10, e -> new Thread(null, e, "Scheduled-ThreadPool"));
        scheduledTaskRegistrar.setScheduler(scheduledExecutorService);
        log.info("【开启多线程定时器worker】");
    }
}

