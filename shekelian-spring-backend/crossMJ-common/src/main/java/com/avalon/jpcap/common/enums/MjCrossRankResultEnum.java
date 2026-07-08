package com.avalon.jpcap.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public enum MjCrossRankResultEnum {

    /**其他>0的值皆为前面还有多少人排队*/
    RANK_RESULT_SUCCESS(-1, "图片处理完成"),
    RANK_PRCESSING(0,"排队完成，正在处理"),
    RANK_RESULT_FAIL(-2, "排队完成但最终绘图失败"),

    ;
    private final Integer code;
    private final String msg;

    public static MjDiscordSendTypeEnum getByCode(int code) {
        for (MjDiscordSendTypeEnum enu : MjDiscordSendTypeEnum.values()) {
            if (enu.getCode() == code) {
                return enu;
            }
        }
        return null;
    }

    public static boolean notInThem(int code){
        for (MjDiscordSendTypeEnum enu : MjDiscordSendTypeEnum.values()) {
            if (enu.getCode() == code) {
                return false;
            }
        }
        return true;
    }
}
