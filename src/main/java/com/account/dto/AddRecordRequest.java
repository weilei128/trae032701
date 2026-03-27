package com.account.dto;

import com.account.constant.RecordConstant;
import lombok.Data;

import javax.validation.constraints.*;

/**
 * 添加收支记录请求
 */
@Data
public class AddRecordRequest {
    
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须为正数")
    private Double amount;
    
    @NotBlank(message = "类型不能为空")
    private String type;
    
    @NotBlank(message = "分类不能为空")
    private String category;
    
    @Size(max = 200, message = "备注最长200个字符")
    private String remark;
    
    public void validate() {
        if (!RecordConstant.isValidType(this.type)) {
            throw new IllegalArgumentException("类型必须是" + RecordConstant.TYPES);
        }
        if (!RecordConstant.isValidCategory(this.type, this.category)) {
            throw new IllegalArgumentException("分类不在预设范围内");
        }
    }
}
