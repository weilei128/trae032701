package com.account.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应
 * @param <T> 数据类型
 */
@Data
public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<T> list;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private Integer pages;
    
    public PageResult() {}
    
    public PageResult(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (int) Math.ceil((double) total / pageSize);
    }
}
