package com.campus.trade.enums;

/**
 * 订单状态枚举
 * 0-已取消 1-待接单 2-已接单 3-已完成
 *
 * @author lu
 * @since 2026-08-16
 */
public enum OrderStatusEnum {

    /** 已取消 */
    CANCELED(0, "已取消"),

    /** 待接单 */
    PENDING(1, "待接单"),

    /** 已接单 */
    ACCEPTED(2, "已接单"),

    /** 已完成 */
    COMPLETED(3, "已完成");

    /** 状态码 */
    private final int code;

    /** 状态描述 */
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据状态码获取枚举，未知状态码返回 null
     */
    public static OrderStatusEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
