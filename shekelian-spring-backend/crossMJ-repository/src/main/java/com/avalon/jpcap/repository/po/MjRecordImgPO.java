package com.avalon.jpcap.repository.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * mj生成图片的记录
 * @author DingHaoLun
 * @since 2023-04-20 20:27
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MjRecordImgPO extends BasePO{

    /**
     * 主键id
     */
    private Long id;

    /*父图片信息 start*/

    /**
     * 父图片主键id
     */
    private Long fatherId;

    /**
     * 父图片中的4个子图的序号;
     */
    private Integer childImgIndex;

    /*父图片信息 end*/

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 请求时候的stream 的messageId
     */
    private String messageId;

    /**
     * 请求时用到的midjourney账号
     */
    private String mjAccount;

    /**
     * 图片discord cdn
     */
    private String imgUrlOuter;

    /**
     * 图片国内cdn
     */
    private String imgUrl;

    /**
     * @see com.avalon.jpcap.common.enums.MjDiscordSendTypeEnum
     */
    private Integer type;

    /**
     * 是否被用户看过
     */
    private Boolean used;
}