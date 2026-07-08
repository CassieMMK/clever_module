package com.avalon.jpcap.domain.old;

import com.avalon.jpcap.common.domain.BaseControllerDomain;
import lombok.Data;

/**
 * @author DingHaoLun
 * @since 2023-02-07 16:58
 **/
@Data
public class GameUserInfoVO extends BaseControllerDomain {

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户头像
     */
    private String userImg;

    /**
     * 所属省份
     */
    private String province;
}
