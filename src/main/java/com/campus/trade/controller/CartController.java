package com.campus.trade.controller;

import com.campus.trade.common.result.Result;
import com.campus.trade.entity.UserDO;
import com.campus.trade.service.CartService;
import com.campus.trade.vo.CartVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public Result<List<CartVO>> list(@AuthenticationPrincipal UserDO user) {
        return Result.success(cartService.list(user.getId()));
    }

    @PostMapping
    public Result<Void> add(@AuthenticationPrincipal UserDO user, @RequestBody Map<String, Object> body) {
        Long productId = ((Number) body.get("productId")).longValue();
        Integer quantity = body.containsKey("quantity") ? ((Number) body.get("quantity")).intValue() : 1;
        cartService.add(user.getId(), productId, quantity);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@AuthenticationPrincipal UserDO user, @PathVariable Long id,
                                @RequestBody Map<String, Object> body) {
        Integer quantity = body.containsKey("quantity") ? ((Number) body.get("quantity")).intValue() : null;
        Integer isSelected = body.containsKey("isSelected") ? ((Number) body.get("isSelected")).intValue() : null;
        cartService.update(user.getId(), id, quantity, isSelected);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@AuthenticationPrincipal UserDO user, @PathVariable Long id) {
        cartService.remove(user.getId(), id);
        return Result.success();
    }
}
