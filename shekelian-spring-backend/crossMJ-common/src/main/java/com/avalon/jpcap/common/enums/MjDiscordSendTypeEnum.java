package com.avalon.jpcap.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MjDiscordSendTypeEnum {

    GENERATE(1, "生成"),
    SCALE(2,"变换大小"),
    VARIATION(3, "变形"),

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
}
