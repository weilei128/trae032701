package com.account.dto;

import lombok.Data;

/**
 * 查询收支记录请求
 */
@Data
public class QueryRecordRequest {
    
    private String startDate;
    
    private String endDate;
    
    private String type;
    
    private String category;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 10;
}
