package com.account.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 收支记录实体
 */
@Data
public class Record implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String userId;
    private Double amount;
    private String type;
    private String category;
    private String remark;
    private String createTime;
    
    public Record() {}
    
    public Record(String id, String userId, Double amount, String type, String category, String remark, String createTime) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.remark = remark;
        this.createTime = createTime;
    }
    
    public String[] toArray() {
        return new String[]{id, userId, String.valueOf(amount), type, category, remark != null ? remark : "", createTime};
    }
    
    public static Record fromArray(String[] arr) {
        if (arr == null || arr.length < 7) {
            return null;
        }
        Record record = new Record();
        record.setId(arr[0]);
        record.setUserId(arr[1]);
        record.setAmount(Double.parseDouble(arr[2]));
        record.setType(arr[3]);
        record.setCategory(arr[4]);
        record.setRemark(arr[5]);
        record.setCreateTime(arr[6]);
        return record;
    }
}
