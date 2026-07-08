package com.avalon.jpcap.common.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * @author DingHaoLun
 * @since 2023-02-02 17:04
 **/
@Data
@Accessors(chain = true)
public class BaseDtoDomain {

    /**
     * 扩展数据
     */
    private Object ext;
}