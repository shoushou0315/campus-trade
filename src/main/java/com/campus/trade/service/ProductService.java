package com.campus.trade.service;

import com.campus.trade.common.result.PageResult;
import com.campus.trade.dto.request.ProductQueryDTO;
import com.campus.trade.dto.request.ProductSaveDTO;
import com.campus.trade.vo.ProductDetailVO;
import com.campus.trade.vo.ProductVO;

public interface ProductService {

    PageResult<ProductVO> search(ProductQueryDTO query);

    ProductDetailVO getDetail(Long id);

    ProductVO create(Long sellerId, ProductSaveDTO dto);

    ProductVO update(Long id, Long sellerId, ProductSaveDTO dto);

    void updateStatus(Long id, Long sellerId, Integer status);

    void delete(Long id, Long sellerId);
}
