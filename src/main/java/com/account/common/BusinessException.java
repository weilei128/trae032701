package com.account.common;

/**
 * 业务异常类
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public Integer getCode() {
        return code;
    }
}
