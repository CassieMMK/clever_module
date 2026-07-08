package com.avalon.jpcap.domain;

import com.avalon.jpcap.common.domain.BaseDtoDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 会员权益dto
 *
 * @author DingHaoLun
 * @since 2023-04-17 16:03
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MjMemberDto extends BaseDtoDomain {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户会员等级
     * @see com.avalon.jpcap.common.enums.MjMemberLevelEnum
     */
    public Integer memberLevel;

    /**
     * 用户会员权益是否有效(通过worker定期扫描更新)
     */
    public Boolean isActivate;

    /**
     * 到期时间
     */
    public Date expireTime;

    /**
     * 用户剩余积分
     */
    public Integer credit;
}