package com.campus.trade.mq.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 下单消息体（异步落库 + 延迟关单共用）
 */
public class OrderCreateMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 买家ID */
    private Long buyerId;
    /** 购物车项ID列表 */
    private List<Long> cartIds;
    /** 备注 */
    private String remark;
    /** 订单号（幂等键：重复消费忽略） */
    private String orderNo;
    /** 消息类型：CREATE=异步落库, CANCEL_CHECK=延迟关单检查 */
    private String type;

    public static final String TYPE_CREATE = "CREATE";
    public static final String TYPE_CANCEL_CHECK = "CANCEL_CHECK";

    public OrderCreateMessage() {
    }

    public OrderCreateMessage(Long buyerId, List<Long> cartIds, String remark, String orderNo, String type) {
        this.buyerId = buyerId;
        this.cartIds = cartIds;
        this.remark = remark;
        this.orderNo = orderNo;
        this.type = type;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public List<Long> getCartIds() {
        return cartIds;
    }

    public void setCartIds(List<Long> cartIds) {
        this.cartIds = cartIds;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "OrderCreateMessage{buyerId=" + buyerId + ", cartIds=" + cartIds
                + ", orderNo='" + orderNo + "', type='" + type + "'}";
    }
}
