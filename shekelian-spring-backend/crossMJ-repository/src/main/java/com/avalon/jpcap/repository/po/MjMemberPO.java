package com.avalon.jpcap.repository.po;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * mj会员服务
 *
 * @author DingHaoLun
 * @since 2023-04-17 16:13
 **/
@Data
@Accessors(chain = true)
public class MjMemberPO extends BasePO{

    /**
     * 主键id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户会员等级
     */
    private Integer memberLevel;

    /**
     * 包月包年等套餐到期时间
     */
    private Date expireTime;

    /**
     * 剩余使用积分数
     */
    private Integer credit;

    /**
     * 用户会员等级是否有效
     * 0 -过期
     * 1 -有效
     * 2 -被冻结
     */
    private Integer isActivate;
}