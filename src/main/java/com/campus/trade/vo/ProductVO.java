package com.campus.trade.vo;

import com.campus.trade.entity.ProductDO;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductVO {

    private Long id;
    private Long sellerId;
    private String sellerName;
    private Long categoryId;
    private String title;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String images;
    private String campus;
    private Integer condition;
    private Integer status;
    private Integer viewCount;
    private Integer collectCount;
    private LocalDateTime gmtCreate;

    public ProductVO() {}

    public static ProductVO from(ProductDO p) {
        ProductVO vo = new ProductVO();
        vo.id = p.getId();
        vo.sellerId = p.getSellerId();
        vo.categoryId = p.getCategoryId();
        vo.title = p.getTitle();
        vo.price = p.getPrice();
        vo.originalPrice = p.getOriginalPrice();
        vo.images = p.getImages();
        vo.campus = p.getCampus();
        vo.condition = p.getCondition();
        vo.status = p.getStatus();
        vo.viewCount = p.getViewCount();
        vo.collectCount = p.getCollectCount();
        vo.gmtCreate = p.getGmtCreate();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public Integer getCondition() { return condition; }
    public void setCondition(Integer condition) { this.condition = condition; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public Integer getCollectCount() { return collectCount; }
    public void setCollectCount(Integer collectCount) { this.collectCount = collectCount; }
    public LocalDateTime getGmtCreate() { return gmtCreate; }
    public void setGmtCreate(LocalDateTime gmtCreate) { this.gmtCreate = gmtCreate; }
}
