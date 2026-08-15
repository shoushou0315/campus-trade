package com.campus.trade.enums;

/**
 * 商品状态枚举
 * 0-已下架 1-在售 2-已售
 *
 * @author lu
 * @since 2026-08-16
 */
public enum ProductStatusEnum {

    /** 已下架 */
    OFF_SHELF(0, "已下架"),

    /** 在售 */
    ON_SALE(1, "在售"),

    /** 已售 */
    SOLD(2, "已售");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    ProductStatusEnum(int code, String desc) {
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
