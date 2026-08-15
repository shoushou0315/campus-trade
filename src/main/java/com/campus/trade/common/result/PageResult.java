package com.campus.trade.common.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PageResult<T> {

    private long total;
    private int pageNum;
    private int pageSize;
    private List<T> list;

    @JsonCreator
    public PageResult(@JsonProperty("total") long total,
                      @JsonProperty("pageNum") int pageNum,
                      @JsonProperty("pageSize") int pageSize,
                      @JsonProperty("list") List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.list = list;
    }

    public long getTotal() { return total; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public List<T> getList() { return list; }
}
