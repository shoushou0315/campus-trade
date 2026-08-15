package com.campus.trade.service;

import com.campus.trade.vo.CartVO;
import java.util.List;

public interface CartService {
    List<CartVO> list(Long userId);
    void add(Long userId, Long productId, Integer quantity);
    void update(Long userId, Long cartId, Integer quantity, Integer isSelected);
    void remove(Long userId, Long cartId);
}
