package com.campus.trade.enums;

/**
 * 用户状态枚举
 * 0-禁用 1-启用
 *
 * @author lu
 * @since 2026-08-16
 */
public enum UserStatusEnum {

    /** 禁用 */
    DISABLED(0, "禁用"),

    /** 启用 */
    ENABLED(1, "启用");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    UserStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
