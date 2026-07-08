package com.avalon.jpcap.repository.po;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author DingHaoLun
 * @since 2022-09-30 16:16
 **/
@Data
public class BasePO implements Serializable {

    /**
     * 扩展数据
     */
    private String extJson;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 上次修改时间
     */
    private Date modifiedTime;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 修改人
     */
    private String modifier;

    /**
     * 数据是否有效
     */
    private Boolean yn;
}