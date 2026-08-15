package com.campus.trade.controller;

import com.campus.trade.common.result.Result;
import com.campus.trade.entity.UserDO;
import com.campus.trade.service.OrderService;
import com.campus.trade.vo.OrderDetailVO;
import com.campus.trade.vo.OrderVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderVO> create(@AuthenticationPrincipal UserDO user,
                                   @RequestBody Map<String, Object> body) {
        List<Long> cartIds = ((List<Integer>) body.get("cartIds"))
                .stream().map(Integer::longValue).toList();
        String remark = (String) body.getOrDefault("remark", null);
        return Result.success(orderService.create(user.getId(), cartIds, remark));
    }

    @GetMapping
    public Result<List<OrderVO>> buyerOrders(@AuthenticationPrincipal UserDO user) {
        return Result.success(orderService.buyerOrders(user.getId()));
    }

    @GetMapping("/sold")
    public Result<List<OrderVO>> sellerOrders(@AuthenticationPrincipal UserDO user) {
        return Result.success(orderService.sellerOrders(user.getId()));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.success(orderService.getDetail(id));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDO user,
                                      @RequestParam Integer status) {
        orderService.updateStatus(id, user.getId(), status);
        return Result.success();
    }
}
