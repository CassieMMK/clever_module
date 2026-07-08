package com.avalon.jpcap.security.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @author DingHaoLun
 * @since 2022-11-08 22:02
 **/
@Data
@AllArgsConstructor
public class TokenHeaderVO implements Serializable {

    /**
     * header中的key
     */
    private String headerKey;

    /**
     * token
     */
    private String token;
}