package com.avalon.jpcap.rpc.wx.service;

import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.common.utils.http.HttpUtils;
import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.rpc.wx.domain.WxAccessVO;
import com.avalon.jpcap.rpc.wx.domain.WxUserInfoVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.avalon.jpcap.common.enums.ResultCodeEnum.RPC_ERROR;

/**
 * @author DingHaoLun
 * @since 2022-11-07 20:41
 **/
@Service
@Slf4j
public class WxAccessRpc {

    /**
     * 公众号唯一标识
     */
    @Value("${wx.appId}")
    private String appId;

    /**
     * 公众号的appSecret
     */
    @Value("${wx.appSecret}")
    private String appSecret;

    /**微信获取信息的权限范围*/
    @Value("${wx.userinfo.permiss}")
    private String userPermiss;

    /**
     * 【小程序专用】
     */
    public WxUserInfoVO getWxUserByMP(String code){
        WxAccessVO wxAccessVO = this.getWxAccessByMP(code);
        return this.getWxUserInfo(wxAccessVO);
    }

    /**
     * 【网页扫码、微信公众号、app获取微信信息用】
     */
    public WxUserInfoVO getWxUser(String code){
        WxAccessVO wxAccessVO = this.getWxAccess(code);
        return this.getWxUserInfo(wxAccessVO);
    }

    /**
     * 通过access_token进一步获取用户的微信个人信息
     * 【作用域为 snsapi_userinfo】
     * @param wxAccessVO
     * @return
     */
    @SneakyThrows
    private WxUserInfoVO getWxUserInfo(WxAccessVO wxAccessVO){
        String url = "https://api.weixin.qq.com/sns/userinfo?"
                + "access_token=" + wxAccessVO.getAccess_token() + "&openid=" +wxAccessVO.getOpenid()
                + "&lang=zh_CN";

        String rs = HttpUtils.get(url);
        WxUserInfoVO wxUserInfoVO = JsonUtils.fromJson(rs, WxUserInfoVO.class);
        if(Objects.isNull(wxUserInfoVO) || StringUtils.isBlank(wxUserInfoVO.getOpenid())){
            log.info("param:{},从微信获取的用户信息为：{}",JsonUtils.toJson(wxAccessVO),JsonUtils.toJson(wxUserInfoVO));
            throw new BusinessException(RPC_ERROR.getCode(),"获取微信用户信息失败");
        }
        return wxUserInfoVO;

//        //暂时不需要额外信息，只要openId作为第三方pin即可
//        return new WxUserInfoVO().setOpenid(wxAccessVO.getOpenid());
    }


    /**
     * 小程序获取
     */
    @SneakyThrows
    private WxAccessVO getWxAccessByMP(String code){
        //并且应用授权作用域userPermiss为snsapi_base时，不弹出授权页面，直接跳转，只能获取用户openid；
        // 而在作用域为snsapi_userinfo时弹出授权页面，后续可通过openid拿到昵称、性别、所在地。
        String url = "https://api.weixin.qq.com/sns/jscode2session?";
        url += ("appid=" + appId);//自己的appid
        url += ("&secret=" + appSecret);//自己的appSecret
        url += "&js_code=" + code;
        url += "&grant_type=authorization_code";
        url += ("&scope=" + userPermiss);
        url += "&connect_redirect=1";

        String rs = HttpUtils.get(url);
        WxAccessVO wxAccessVO = JsonUtils.fromJson(rs, WxAccessVO.class);

        String openid = wxAccessVO.getOpenid();
        if(StringUtils.isBlank(openid)){
            log.info("param:{},从微信获取的登录信息为：{}",code,JsonUtils.toJson(wxAccessVO));
            throw new BusinessException(RPC_ERROR.getCode(),wxAccessVO.getErrcode() + wxAccessVO.getErrmsg()+"获取微信登录信息失败");
        }

        return wxAccessVO;
    }

    /**
     * 公众号、网页扫码、app获取
     */
    @SneakyThrows
    private WxAccessVO getWxAccess(String code){
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?";
        url += ("appid=" + appId);//自己的appid
        url += ("&secret=" + appSecret);//自己的appSecret
        url += "&code=" + code;
        url += "&grant_type=authorization_code";

        String rs = HttpUtils.get(url);
        WxAccessVO wxAccessVO = JsonUtils.fromJson(rs, WxAccessVO.class);

        String openid = wxAccessVO.getOpenid();
        if(StringUtils.isBlank(openid)){
            log.info("param:{},从微信获取的登录信息为：{}",code,JsonUtils.toJson(wxAccessVO));
            throw new BusinessException(RPC_ERROR.getCode(),wxAccessVO.getErrcode() + wxAccessVO.getErrmsg()+"获取微信登录信息失败");
        }

        return wxAccessVO;
    }
}