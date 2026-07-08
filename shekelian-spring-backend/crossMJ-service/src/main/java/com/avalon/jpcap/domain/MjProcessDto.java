package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseDtoDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排队及生成进度
 *
 * @author DingHaoLun
 * @since 2023-04-19 14:38
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MjProcessDto extends BaseDtoDomain {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 队列流名称
     */
    private String streamKey;

    /**
     * 队列stream流中的Id
     */
    private String messageId;

    /**
     * 排队排名
     * 0-为处理中或已完成，1为前面还有1人排队。依此类推
     */
    private Integer rank;

    /**
     * 请求类型
     * @see com.avalon.jpcap.common.enums.MjDiscordSendTypeEnum
     */
    private Integer type;

    /**
     * 请求时的prompt
     */
    private String prompt;
}