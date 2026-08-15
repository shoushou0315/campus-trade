package com.campus.trade.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ProductDetailVO {

    private ProductVO product;
    private UserVO seller;
    private CategoryVO category;

    public ProductDetailVO() {}

    @JsonCreator
    public ProductDetailVO(@JsonProperty("product") ProductVO product,
                           @JsonProperty("seller") UserVO seller,
                           @JsonProperty("category") CategoryVO category) {
        this.product = product;
        this.seller = seller;
        this.category = category;
    }

    public ProductVO getProduct() { return product; }
    public UserVO getSeller() { return seller; }
    public CategoryVO getCategory() { return category; }
}
