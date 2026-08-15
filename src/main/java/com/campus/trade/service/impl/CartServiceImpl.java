package com.campus.trade.service.impl;

import com.campus.trade.common.exception.BusinessException;
import com.campus.trade.entity.CartDO;
import com.campus.trade.entity.ProductDO;
import com.campus.trade.enums.ProductStatusEnum;
import com.campus.trade.mapper.CartMapper;
import com.campus.trade.mapper.ProductMapper;
import com.campus.trade.service.CartService;
import com.campus.trade.vo.CartVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    public CartServiceImpl(CartMapper cartMapper, ProductMapper productMapper) {
        this.cartMapper = cartMapper;
        this.productMapper = productMapper;
    }

    @Override
    public List<CartVO> list(Long userId) {
        return cartMapper.selectByUserId(userId).stream().map(c -> {
            ProductDO p = productMapper.selectById(c.getProductId());
            CartVO vo = new CartVO();
            vo.setId(c.getId());
            vo.setProductId(c.getProductId());
            vo.setQuantity(c.getQuantity());
            vo.setIsSelected(c.getIsSelected());
            if (p != null) {
                vo.setProductTitle(p.getTitle());
                vo.setPrice(p.getPrice());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void add(Long userId, Long productId, Integer quantity) {
        ProductDO product = productMapper.selectById(productId);
            if (product == null || product.getStatus() != ProductStatusEnum.ON_SALE.getCode()) {
            throw new BusinessException("商品不存在或已下架");
        }
        CartDO exist = cartMapper.selectByUserAndProduct(userId, productId);
        if (exist != null) {
            cartMapper.update(exist.getId(), exist.getQuantity() + quantity, 1);
        } else {
            CartDO cart = new CartDO();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity != null ? quantity : 1);
            cart.setIsSelected(1);
            cartMapper.insert(cart);
        }
    }

    @Override
    public void update(Long userId, Long cartId, Integer quantity, Integer isSelected) {
        cartMapper.update(cartId, quantity, isSelected);
    }

    @Override
    public void remove(Long userId, Long cartId) {
        cartMapper.deleteById(cartId);
    }
}
