package com.avalon.jpcap.repository.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * @author DingHaoLun
 * @since 2023-04-20 20:43
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class MjRecordImgQueryPO implements Serializable {

    /**
     * 主键id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * stream放进去时的messageId
     */
    private String messageId;

    /**
     * 是否被用户已经看过
     */
    private Boolean used;
}