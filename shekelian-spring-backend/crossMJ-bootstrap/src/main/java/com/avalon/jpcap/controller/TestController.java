package com.avalon.jpcap.controller;

import com.avalon.jpcap.common.filter.LoginContextHelper;
import com.avalon.jpcap.common.filter.vo.LoginVO;
import com.avalon.jpcap.common.result.HttpResult;
import io.swagger.annotations.Api;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author DingHaoLun
 * @since 2022-11-12 18:06
 **/
@RestController
@Validated
@RequestMapping("/avalon/")
@Api(tags = "单测接口")
public class TestController {

    private List<Long> ids1;
    private String str = "hello world!";
    @Value("${error.activityIds}")
    public void setIds(List<Long> ids){
        if(CollectionUtils.isNotEmpty(ids)){
            ids1 = ids;
        } else {
            ids1 = null;
        }
    }

    /**
     * 校验登录态是否正常，输出登录态的信息
     */
    @GetMapping("/testLoginInfo")
    public HttpResult<String> getLoginInfo(){
        LoginVO loginVO = LoginContextHelper.loadLoginInfo();
        return HttpResult.success(String.format("平台为%s,pin为%s", loginVO.getPlatform(),loginVO.getPlatformPin()));
    }

    /**
     * 校验不需要登录态的接口
     */
    @GetMapping("/needNotLogin")
    public HttpResult<Void> needNotLogin(){
        System.out.println(ids1);
        return HttpResult.success(null);
    }

    @GetMapping("/simpleTest")
    public HttpResult<String> simpleTest(){
        System.out.println("simple test");
        return HttpResult.success("hello world!");
    }
}