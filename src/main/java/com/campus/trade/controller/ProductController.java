package com.campus.trade.controller;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.common.result.Result;
import com.campus.trade.dto.request.ProductQueryDTO;
import com.campus.trade.dto.request.ProductSaveDTO;
import com.campus.trade.entity.UserDO;
import com.campus.trade.service.ProductService;
import com.campus.trade.vo.ProductDetailVO;
import com.campus.trade.vo.ProductVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<PageResult<ProductVO>> search(@Valid ProductQueryDTO query) {
        return Result.success(productService.search(query));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        return Result.success(productService.getDetail(id));
    }

    @PostMapping
    public Result<ProductVO> create(@AuthenticationPrincipal UserDO user,
                                     @Valid @RequestBody ProductSaveDTO dto) {
        return Result.success(productService.create(user.getId(), dto));
    }

    @PutMapping("/{id}")
    public Result<ProductVO> update(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDO user,
                                     @Valid @RequestBody ProductSaveDTO dto) {
        return Result.success(productService.update(id, user.getId(), dto));
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDO user,
                                      @RequestParam Integer status) {
        productService.updateStatus(id, user.getId(), status);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                                @AuthenticationPrincipal UserDO user) {
        productService.delete(id, user.getId());
        return Result.success();
    }
}
