package com.campus.trade.vo;

import java.util.List;

public class OrderDetailVO {
    private OrderVO order;
    private UserVO buyer;
    private UserVO seller;
    private List<OrderItemVO> items;

    public OrderVO getOrder() { return order; }
    public void setOrder(OrderVO order) { this.order = order; }
    public UserVO getBuyer() { return buyer; }
    public void setBuyer(UserVO buyer) { this.buyer = buyer; }
    public UserVO getSeller() { return seller; }
    public void setSeller(UserVO seller) { this.seller = seller; }
    public List<OrderItemVO> getItems() { return items; }
    public void setItems(List<OrderItemVO> items) { this.items = items; }
}
