package com.campus.trade.mapper;

import com.campus.trade.entity.ProductDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {

    List<ProductDO> search(@Param("keyword") String keyword,
                           @Param("categoryId") Long categoryId,
                           @Param("campus") String campus,
                           @Param("minPrice") java.math.BigDecimal minPrice,
                           @Param("maxPrice") java.math.BigDecimal maxPrice,
                           @Param("condition") Integer condition,
                           @Param("sortBy") String sortBy,
                           @Param("offset") int offset,
                           @Param("limit") int limit);

    int countSearch(@Param("keyword") String keyword,
                    @Param("categoryId") Long categoryId,
                    @Param("campus") String campus,
                    @Param("minPrice") java.math.BigDecimal minPrice,
                    @Param("maxPrice") java.math.BigDecimal maxPrice,
                    @Param("condition") Integer condition);

    ProductDO selectById(@Param("id") Long id);

    int insert(ProductDO product);

    int update(ProductDO product);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int incrementViewCount(@Param("id") Long id);
}
