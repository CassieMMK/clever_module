package com.avalon.jpcap.controller;

import com.avalon.jpcap.common.enums.LoginPlatformSourceEnum;
import com.avalon.jpcap.common.enums.ResultCodeEnum;
import com.avalon.jpcap.common.filter.LoginContextHelper;
import com.avalon.jpcap.common.result.HttpResult;
import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.converter.UserInfoConverter;
import com.avalon.jpcap.domain.MjMemberDto;
import com.avalon.jpcap.domain.old.MemberFundVO;
import com.avalon.jpcap.domain.old.UserInfoVO;
import com.avalon.jpcap.domain.old.UserLoginStateVO;
import com.avalon.jpcap.domain.old.UserLoginVO;
import com.avalon.jpcap.rpc.wx.domain.WxUserInfoVO;
import com.avalon.jpcap.security.domain.dto.UserInfoDTO;
import com.avalon.jpcap.security.domain.vo.TokenHeaderVO;
import com.avalon.jpcap.security.service.TokenService;
import com.avalon.jpcap.security.service.XlUserService;
import com.avalon.jpcap.service.MjMemberService;
import com.avalon.jpcap.service.ThirdPartyLoginService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author DingHaoLun
 * @since 2022-11-01 11:26
 **/

@RestController
@Validated
@RequestMapping("/avalon")
@Api(tags = "登录接口")
public class UserController {

    /**第三方登录服务*/
    @Resource
    private ThirdPartyLoginService thirdPartyLoginService;

    /**Jwt token生成服务*/
    @Resource
    private TokenService tokenService;

    /**用户登录信息保存*/
    @Resource
    private XlUserService xlUserService;

    /**用户会员服务*/
    @Resource
    private MjMemberService mjMemberService;

    @Value("${cookie.expire}")
    private Integer cookieMaxAge;


    @GetMapping("/getXXX")
    @ApiOperation("获取用户信息")
    public HttpResult<UserInfoVO> getXXX(){
        Long userId = LoginContextHelper.loadLoginInfo().getUserId();
        //通过userId查询用户信息
        UserInfoDTO userInfoDTO = xlUserService.selectUserById(userId);
        List<MjMemberDto> mjMemberDtoList = mjMemberService.queryMember(userId);
        return HttpResult.success(UserInfoConverter.dto2Vo(userInfoDTO, mjMemberDtoList));
    }

    @GetMapping("/getUserInfo")
    @ApiOperation("获取用户信息")
    public HttpResult<UserInfoVO> getUserInfo(){
        Long userId = LoginContextHelper.loadLoginInfo().getUserId();
        //通过userId查询用户信息
        UserInfoDTO userInfoDTO = xlUserService.selectUserById(userId);
        List<MjMemberDto> mjMemberDtoList = mjMemberService.queryMember(userId);
        return HttpResult.success(UserInfoConverter.dto2Vo(userInfoDTO, mjMemberDtoList));
    }

    @PostMapping("/saveUserInfo")
    @ApiOperation("保存、更新用户昵称和头像")
    public HttpResult<Void> updateUserInfo(@RequestBody @Valid UserInfoVO vo){
        Long userId = LoginContextHelper.loadLoginInfo().getUserId();
        Boolean success = xlUserService.update(UserInfoConverter.vo2Dto(vo,userId));
        if(success){
            return HttpResult.success(null);
        }
        return HttpResult.failure(ResultCodeEnum.FAIL.getCode(), "用户昵称头像更新失败");
    }

    @PostMapping("/memberFund")
    @ApiOperation("会员充值")
    public HttpResult<Void> memberFund(@RequestBody @Valid MemberFundVO vo){
        return null;
    }

    @PostMapping("/loginMP")
    @ApiOperation("[微信小程序专用]用户登录，刷新token（key为 X-token），data返回新用户-ture  老用户-false")
    public HttpResult<Boolean> loginMP(@RequestBody @Valid UserLoginVO userLoginVO) throws IOException {
        Boolean newMan = Boolean.FALSE;//是否是新用户
        String wxcode = userLoginVO.getWxcode(); //wxcode用于请求用户信息，是一次性使用的。
        WxUserInfoVO wxUserInfoVO = thirdPartyLoginService.getWxInfoByMP(wxcode);

        //1、用户若是第一次登录，则会保存到注册用户数据库中记录（会做判断）
        //2、若不是第一次登录，返回用户id
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        Map<String,String> extMap = new HashMap<>();
        extMap.put("wxUnionId",wxUserInfoVO.getUnionid());//若有unionId则要存到ext中
        userInfoDTO.setPlatform(LoginPlatformSourceEnum.WX.getCode()) //platform是微信
                .setPlatformPin(wxUserInfoVO.getOpenid()) //openId在微信中是一个用户一个，唯一不变
                .setNickName(wxUserInfoVO.getNickname())
                .setHeadImgUrl(wxUserInfoVO.getHeadimgurl())
                .setExt(extMap);

        Long userId = xlUserService.register(userInfoDTO);

        //2、生成token并放入cookie中
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        HttpServletResponse response = servletRequestAttributes.getResponse();
        response = createJwtCookie(wxUserInfoVO, Objects.requireNonNull(response));

        //3、配置cookie到response中就行，controller在servlet内部，servlet会自动包装response返回去。
        //controller中如果返回某个Object的话，会自动放进response的body中
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        return HttpResult.success(newMan);
    }

    /**
     * @param code  微信登录code
     * @param state 额外信息的Json
     * @return
     * @throws IOException
     */
    @GetMapping("/login")
    @ApiOperation("[微信生态登录]用户登录，刷新token（key为 X-token），data返回新用户-ture  老用户-false")
    public void login(@RequestParam String code, @RequestParam String state) throws IOException {

        String wxcode = code; //wxcode用于请求用户信息，是一次性使用的。
        UserLoginStateVO stateVO = JsonUtils.fromJson(state, UserLoginStateVO.class);

        WxUserInfoVO wxUserInfoVO = thirdPartyLoginService.getWxInfo(wxcode);

        //1、用户若是第一次登录，则会保存到注册用户数据库中记录（会做判断）
        //2、若不是第一次登录，返回用户id
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        Map<String,String> extMap = new HashMap<>();
        extMap.put("wxUnionId",wxUserInfoVO.getUnionid());//若有unionId则要存到ext中
        userInfoDTO.setPlatform(LoginPlatformSourceEnum.WX.getCode()) //platform是微信
                .setPlatformPin(wxUserInfoVO.getOpenid()) //openId在微信中是一个用户一个，唯一不变
                .setNickName(wxUserInfoVO.getNickname())
                .setHeadImgUrl(wxUserInfoVO.getHeadimgurl())
                .setExt(extMap);

        Long userId = xlUserService.register(userInfoDTO);

        //2、生成cookie并放入cookie中
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        HttpServletResponse response = servletRequestAttributes.getResponse();
        response = createJwtCookie(wxUserInfoVO, Objects.requireNonNull(response));

        //3、配置cookie到response中就行，controller在servlet内部，servlet会自动包装response返回去。
        //controller中如果返回某个Object的话，会自动放进response的body中

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        //重定向，重定向之后直接就返回response了
        response.sendRedirect(stateVO.getRedirectURL());
    }



    /**
     * 将用户登录token存入header中某字段
     */
    private HttpServletResponse createJwtToken(WxUserInfoVO wxUserInfoVO, HttpServletResponse response){
        Claims claims = new DefaultClaims().setIssuer(LoginPlatformSourceEnum.WX.getCode().toString()).setId(wxUserInfoVO.getOpenid());
        TokenHeaderVO tokenHeaderVO = tokenService.createJwt(claims);

        response.setHeader(tokenHeaderVO.getHeaderKey(), tokenHeaderVO.getToken());
        return response;
    }

    /**
     * 将用户登录token存入cookie中
     */
    private HttpServletResponse createJwtCookie(WxUserInfoVO wxUserInfoVO, HttpServletResponse response){
        Claims claims = new DefaultClaims().setIssuer(LoginPlatformSourceEnum.WX.getCode().toString()).setId(wxUserInfoVO.getOpenid());
        TokenHeaderVO tokenHeaderVO = tokenService.createJwt(claims);
        Cookie cookie = new Cookie(tokenHeaderVO.getHeaderKey(), tokenHeaderVO.getToken());
        cookie.setMaxAge(cookieMaxAge);//当前 Cookie有效期，不能为0和负数，否则cookie会被删除

        response.addCookie(cookie);
        return response;
    }

}