package com.avalon.jpcap.security.service.impl;

import com.avalon.jpcap.common.enums.TokenErrorEnum;
import com.avalon.jpcap.common.exceptions.BusinessException;
import com.avalon.jpcap.common.utils.JsonUtils;
import com.avalon.jpcap.security.domain.vo.TokenHeaderVO;
import com.avalon.jpcap.security.service.TokenService;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * token服务
 * @author DingHaoLun
 * @since 2022-10-06 15:30
 **/
@Service
@Slf4j
public class TokenServiceImpl implements TokenService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long lifePeriod;

    @Value("${jwt.tokenHeader}")
    private String tokenHeader;

    @Value("${jwt.tokenPrefix}")
    private String tokenPrefix;

    @Override
    public TokenHeaderVO createJwt(Map<String, Object> claims) {
        Date now = new Date();
        //过期时间
        Date expireDate = new Date(now.getTime() + lifePeriod);
        String token = Jwts.builder()
                .setHeaderParam("type","JWT")
                .setId(UUID.randomUUID().toString())//设置jti  是JWT的唯一标识，根据业务需要，这个可以设置为一个不重复的值，主要用来作为一次性token,从而回避重放攻击。
                .setClaims(claims)          //业务内需要存储的参数
                .setIssuedAt(now)           //iat: jwt的签发时间
                .setExpiration(expireDate) // exp: jwt的过期时间，这个过期时间必须要大于签发时间
                .signWith(SignatureAlgorithm.HS512, secret) //设置签名使用的签名算法和签名使用的秘钥
                .compressWith(CompressionCodecs.GZIP)  //数据压缩方式
                .compact();

        return new TokenHeaderVO(tokenHeader, tokenPrefix + token);
    }

    @Override
    public Claims parseJwt(HttpServletRequest httpServletRequest) {
        //通过getHeader获取key为tokenHeader对应的value
        String authHeader = httpServletRequest.getHeader(this.tokenHeader);
        if (StringUtils.isNotBlank(authHeader) && StringUtils.contains(authHeader, this.tokenPrefix)) {
            //截取token前缀之后的真实token值
            String authToken = authHeader.substring(this.tokenPrefix.length());
            try {
                Claims claims = Jwts.parser()
                        .setSigningKey(secret)
                        .parseClaimsJws(authToken)
                        .getBody();
                return claims;
            } catch (ExpiredJwtException e){
                log.info("token已过期,token字符串为{}", JsonUtils.toJson(authToken));
                throw new BusinessException(TokenErrorEnum.TOKEN_PARSE_ERROR.getCode().toString(),TokenErrorEnum.TOKEN_PARSE_ERROR.getMsg());
            } catch (Exception e) {
                log.info("token解析失败:{}, token字符串为{}", JsonUtils.toJson(authToken));
                throw new BusinessException(TokenErrorEnum.TOKEN_PARSE_ERROR.getCode().toString(),TokenErrorEnum.TOKEN_PARSE_ERROR.getMsg());
            }
        }
        throw new BusinessException(TokenErrorEnum.TOKEN_PARSE_ERROR.getCode().toString(),TokenErrorEnum.TOKEN_PARSE_ERROR.getMsg());
    }

    @Override
    public Claims parseJwtCookie(HttpServletRequest httpServletRequest) {
        //通过cookie获取key为tokenHeader对应的value
        Cookie[] cookies = httpServletRequest.getCookies();
        Cookie cookie = findCookie(tokenHeader, cookies);

        //解析
        if (cookie != null) {
            String authToken = cookie.getValue().substring(this.tokenPrefix.length());
            try {
                //截取token前缀之后的真实token值
                Claims claims = Jwts.parser()
                        .setSigningKey(secret)
                        .parseClaimsJws(authToken)
                        .getBody();
                return claims;
            } catch (ExpiredJwtException e){
                log.info("token已过期,token字符串为{}", JsonUtils.toJson(authToken));
                throw new BusinessException(TokenErrorEnum.TOKEN_PARSE_ERROR.getCode().toString(),TokenErrorEnum.TOKEN_PARSE_ERROR.getMsg());
            } catch (Exception e) {
                log.info("token解析失败:{}, token字符串为{}", JsonUtils.toJson(authToken));
                throw new BusinessException(TokenErrorEnum.TOKEN_PARSE_ERROR.getCode().toString(),TokenErrorEnum.TOKEN_PARSE_ERROR.getMsg());
            }
        }
        throw new BusinessException(TokenErrorEnum.TOKEN_PARSE_ERROR.getCode().toString(),TokenErrorEnum.TOKEN_PARSE_ERROR.getMsg());
    }

    /**从cookie中获取指定key的数据*/
    private static Cookie findCookie(String name , Cookie[] cookies){
        if (name == null || cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }
}