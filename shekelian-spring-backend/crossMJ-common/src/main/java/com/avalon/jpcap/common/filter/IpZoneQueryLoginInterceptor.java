package com.avalon.jpcap.common.filter;

import com.avalon.jpcap.common.queue.DefaultAbstractBlockingQueue;
import com.avalon.jpcap.common.thread.ThreadPoolService;
import com.avalon.jpcap.common.utils.IpUtils;
import com.avalon.jpcap.common.utils.QQWry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器，获取用户ip，查询归属地并归档
 */
@Slf4j
@Order(2)
@Component
public class IpZoneQueryLoginInterceptor implements HandlerInterceptor{

    @Resource(name = "defaultThreadPoolService")
    private ThreadPoolService defaultThreadPoolService;

    @Resource(name = "blockingStringQueue")
    private DefaultAbstractBlockingQueue<String> blockingStringQueue;

    private static final QQWry qqWry;
    static {
        qqWry = new QQWry();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try{
            //1、记录http请求来自哪个省或省级市
            String ipAddress = IpUtils.getIpAddress(request);
            String province = qqWry.findIP(ipAddress).getMainInfo();
            //异步写入
            asyncWriteVisitCount(province);
        }catch (Exception ignored){
            ;
        }
        return true;
    }

    /**
     * 异步记录落库,将数据放入阻塞队列中
     */
    public void asyncWriteVisitCount(String province){
        defaultThreadPoolService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    //阻塞队列
                    blockingStringQueue.saveQueueData(province);
                } catch (Exception e) {
                    log.error("用户登录使用，记录到阻塞队列，即使失败也不影响主流程,Exception：", e);
                }
            }
        });
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {}

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex){}
}
