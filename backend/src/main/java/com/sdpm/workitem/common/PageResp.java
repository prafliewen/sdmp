package com.sdpm.workitem.common;

import java.util.List;

public class PageResp<T> {

    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private List<T> records;

    private PageResp() {
    }

    public static <T> PageResp<T> of(Integer pageNo, Integer pageSize, Long total, List<T> records) {
        PageResp<T> p = new PageResp<>();
        p.pageNo = pageNo;
        p.pageSize = pageSize;
        p.total = total;
        p.records = records;
        return p;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public List<T> getRecords() {
        return records;
    }
}