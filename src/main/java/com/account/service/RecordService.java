package com.account.service;

import com.account.common.BusinessException;
import com.account.common.PageResult;
import com.account.config.CsvProperties;
import com.account.dto.AddRecordRequest;
import com.account.dto.QueryRecordRequest;
import com.account.dto.UpdateRecordRequest;
import com.account.entity.Record;
import com.account.util.CsvStorageUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记账服务类
 */
@Service
public class RecordService {
    
    private final CsvStorageUtil csvStorageUtil;
    private final CsvProperties csvProperties;
    
    public RecordService(CsvStorageUtil csvStorageUtil, CsvProperties csvProperties) {
        this.csvStorageUtil = csvStorageUtil;
        this.csvProperties = csvProperties;
    }
    
    /**
     * 添加收支记录
     */
    public Record addRecord(String userId, AddRecordRequest request) {
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
        
        String id = csvStorageUtil.generateId();
        String createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        Record record = new Record(id, userId, request.getAmount(), request.getType(), 
                                   request.getCategory(), request.getRemark(), createTime);
        csvStorageUtil.writeLine(csvProperties.getRecordsFile(), record.toArray());
        
        return record;
    }
    
    /**
     * 更新收支记录
     */
    public Record updateRecord(String userId, String recordId, UpdateRecordRequest request) {
        try {
            request.validate();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
        
        String[] recordArr = csvStorageUtil.findById(csvProperties.getRecordsFile(), 0, recordId);
        if (recordArr == null) {
            throw new BusinessException("记录不存在");
        }
        
        Record record = Record.fromArray(recordArr);
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("权限不足，只能修改自己的记录");
        }
        
        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setRemark(request.getRemark());
        
        csvStorageUtil.updateLine(csvProperties.getRecordsFile(), 0, recordId, record.toArray());
        
        return record;
    }
    
    /**
     * 删除收支记录
     */
    public void deleteRecord(String userId, String recordId) {
        String[] recordArr = csvStorageUtil.findById(csvProperties.getRecordsFile(), 0, recordId);
        if (recordArr == null) {
            throw new BusinessException("记录不存在");
        }
        
        Record record = Record.fromArray(recordArr);
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("权限不足，只能删除自己的记录");
        }
        
        csvStorageUtil.deleteLine(csvProperties.getRecordsFile(), 0, recordId);
    }
    
    /**
     * 查询收支记录
     */
    public PageResult<Record> queryRecords(String userId, QueryRecordRequest request) {
        List<String[]> allRecords = csvStorageUtil.findByCondition(csvProperties.getRecordsFile(), 1, userId);
        
        List<Record> records = allRecords.stream()
                .map(Record::fromArray)
                .filter(r -> filterByCondition(r, request))
                .sorted(Comparator.comparing(Record::getCreateTime).reversed())
                .collect(Collectors.toList());
        
        int total = records.size();
        int start = (request.getPageNum() - 1) * request.getPageSize();
        int end = Math.min(start + request.getPageSize(), total);
        
        List<Record> pageRecords = start < total ? records.subList(start, end) : new ArrayList<>();
        
        return new PageResult<>(pageRecords, (long) total, request.getPageNum(), request.getPageSize());
    }
    
    private boolean filterByCondition(Record record, QueryRecordRequest request) {
        if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
            if (record.getCreateTime().compareTo(request.getStartDate()) < 0) {
                return false;
            }
        }
        
        if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            String endDateTime = request.getEndDate() + " 23:59:59";
            if (record.getCreateTime().compareTo(endDateTime) > 0) {
                return false;
            }
        }
        
        if (request.getType() != null && !request.getType().isEmpty()) {
            if (!record.getType().equals(request.getType())) {
                return false;
            }
        }
        
        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            if (!record.getCategory().equals(request.getCategory())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 获取单条记录
     */
    public Record getRecord(String userId, String recordId) {
        String[] recordArr = csvStorageUtil.findById(csvProperties.getRecordsFile(), 0, recordId);
        if (recordArr == null) {
            throw new BusinessException("记录不存在");
        }
        
        Record record = Record.fromArray(recordArr);
        if (!record.getUserId().equals(userId)) {
            throw new BusinessException("权限不足");
        }
        
        return record;
    }
    
    /**
     * 获取用户所有记录
     */
    public List<Record> getAllRecords(String userId) {
        List<String[]> allRecords = csvStorageUtil.findByCondition(csvProperties.getRecordsFile(), 1, userId);
        return allRecords.stream()
                .map(Record::fromArray)
                .collect(Collectors.toList());
    }
}
