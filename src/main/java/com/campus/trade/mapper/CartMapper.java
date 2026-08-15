package com.campus.trade.mapper;

import com.campus.trade.entity.CartDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface CartMapper {
    List<CartDO> selectByUserId(@Param("userId") Long userId);
    CartDO selectByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);
    int insert(CartDO cart);
    int update(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("isSelected") Integer isSelected);
    int deleteById(@Param("id") Long id);
    int deleteByUserId(@Param("userId") Long userId);
    List<CartDO> selectByIds(@Param("ids") List<Long> ids);
}
