package com.account.service;

import com.account.constant.RecordConstant;
import com.account.entity.Record;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务类
 */
@Service
public class StatisticsService {
    
    private final RecordService recordService;
    
    public StatisticsService(RecordService recordService) {
        this.recordService = recordService;
    }
    
    /**
     * 按周统计收支
     */
    public Map<String, Object> weeklyStatistics(String userId, int year, int week) {
        List<Record> records = recordService.getAllRecords(userId);
        
        double totalIncome = 0;
        double totalExpense = 0;
        
        for (Record record : records) {
            LocalDate recordDate = LocalDate.parse(record.getCreateTime().substring(0, 10), 
                                                   DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            int recordYear = recordDate.get(IsoFields.WEEK_BASED_YEAR);
            int recordWeek = recordDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            
            if (recordYear == year && recordWeek == week) {
                if (RecordConstant.TYPE_INCOME.equals(record.getType())) {
                    totalIncome += record.getAmount();
                } else {
                    totalExpense += record.getAmount();
                }
            }
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("week", week);
        result.put("totalIncome", Math.round(totalIncome * 100) / 100.0);
        result.put("totalExpense", Math.round(totalExpense * 100) / 100.0);
        result.put("balance", Math.round((totalIncome - totalExpense) * 100) / 100.0);
        
        return result;
    }
    
    /**
     * 按月统计收支
     */
    public Map<String, Object> monthlyStatistics(String userId, int year, int month) {
        List<Record> records = recordService.getAllRecords(userId);
        
        double totalIncome = 0;
        double totalExpense = 0;
        
        for (Record record : records) {
            String recordMonth = record.getCreateTime().substring(0, 7);
            String targetMonth = String.format("%d-%02d", year, month);
            
            if (recordMonth.equals(targetMonth)) {
                if (RecordConstant.TYPE_INCOME.equals(record.getType())) {
                    totalIncome += record.getAmount();
                } else {
                    totalExpense += record.getAmount();
                }
            }
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("totalIncome", Math.round(totalIncome * 100) / 100.0);
        result.put("totalExpense", Math.round(totalExpense * 100) / 100.0);
        result.put("balance", Math.round((totalIncome - totalExpense) * 100) / 100.0);
        
        return result;
    }
    
    /**
     * 按分类统计占比
     */
    public Map<String, Object> categoryStatistics(String userId, String type, Integer year, Integer month) {
        List<Record> records = recordService.getAllRecords(userId);
        
        records = records.stream()
                .filter(r -> r.getType().equals(type))
                .filter(r -> {
                    if (year == null) return true;
                    String recordYear = r.getCreateTime().substring(0, 4);
                    return recordYear.equals(String.valueOf(year));
                })
                .filter(r -> {
                    if (month == null) return true;
                    String recordMonth = r.getCreateTime().substring(5, 7);
                    return recordMonth.equals(String.format("%02d", month));
                })
                .collect(Collectors.toList());
        
        Map<String, Double> categoryTotals = new LinkedHashMap<>();
        double totalAmount = 0;
        
        for (Record record : records) {
            String category = record.getCategory();
            double amount = record.getAmount();
            categoryTotals.merge(category, amount, Double::sum);
            totalAmount += amount;
        }
        
        List<Map<String, Object>> categoryList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", entry.getKey());
            item.put("amount", Math.round(entry.getValue() * 100) / 100.0);
            double percentage = totalAmount > 0 ? (entry.getValue() / totalAmount * 100) : 0;
            item.put("percentage", Math.round(percentage * 100) / 100.0);
            categoryList.add(item);
        }
        
        categoryList.sort((a, b) -> Double.compare((Double) b.get("amount"), (Double) a.get("amount")));
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("totalAmount", Math.round(totalAmount * 100) / 100.0);
        result.put("categories", categoryList);
        
        return result;
    }
    
    /**
     * 获取概览统计
     */
    public Map<String, Object> overview(String userId) {
        List<Record> records = recordService.getAllRecords(userId);
        
        double totalIncome = 0;
        double totalExpense = 0;
        
        String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        double monthIncome = 0;
        double monthExpense = 0;
        
        for (Record record : records) {
            if (RecordConstant.TYPE_INCOME.equals(record.getType())) {
                totalIncome += record.getAmount();
            } else {
                totalExpense += record.getAmount();
            }
            
            String recordMonth = record.getCreateTime().substring(0, 7);
            if (recordMonth.equals(currentMonth)) {
                if (RecordConstant.TYPE_INCOME.equals(record.getType())) {
                    monthIncome += record.getAmount();
                } else {
                    monthExpense += record.getAmount();
                }
            }
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalIncome", Math.round(totalIncome * 100) / 100.0);
        result.put("totalExpense", Math.round(totalExpense * 100) / 100.0);
        result.put("totalBalance", Math.round((totalIncome - totalExpense) * 100) / 100.0);
        result.put("monthIncome", Math.round(monthIncome * 100) / 100.0);
        result.put("monthExpense", Math.round(monthExpense * 100) / 100.0);
        result.put("monthBalance", Math.round((monthIncome - monthExpense) * 100) / 100.0);
        result.put("recordCount", records.size());
        
        return result;
    }
}
