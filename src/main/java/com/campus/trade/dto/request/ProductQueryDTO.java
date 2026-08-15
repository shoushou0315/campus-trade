package com.campus.trade.dto.request;

import jakarta.validation.constraints.Min;

public class ProductQueryDTO {

    private String keyword;
    private Long categoryId;
    private String campus;
    private java.math.BigDecimal minPrice;
    private java.math.BigDecimal maxPrice;
    private Integer condition;
    private String sortBy;

    @Min(1)
    private int pageNum = 1;

    @Min(1)
    private int pageSize = 10;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public java.math.BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(java.math.BigDecimal minPrice) { this.minPrice = minPrice; }
    public java.math.BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(java.math.BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public Integer getCondition() { return condition; }
    public void setCondition(Integer condition) { this.condition = condition; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getOffset() { return (pageNum - 1) * pageSize; }
}
