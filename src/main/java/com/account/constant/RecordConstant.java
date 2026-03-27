package com.account.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 收支类型和分类常量
 */
public class RecordConstant {
    
    public static final String TYPE_INCOME = "收入";
    public static final String TYPE_EXPENSE = "支出";
    
    public static final List<String> TYPES = Arrays.asList(TYPE_INCOME, TYPE_EXPENSE);
    
    public static final List<String> INCOME_CATEGORIES = Arrays.asList(
            "薪资", "奖金", "投资收益", "兼职", "红包", "其他收入"
    );
    
    public static final List<String> EXPENSE_CATEGORIES = Arrays.asList(
            "餐饮", "购物", "交通", "娱乐", "医疗", "教育", "住房", "通讯", "水电", "其他支出"
    );
    
    public static List<String> getCategories(String type) {
        if (TYPE_INCOME.equals(type)) {
            return INCOME_CATEGORIES;
        } else if (TYPE_EXPENSE.equals(type)) {
            return EXPENSE_CATEGORIES;
        }
        return Arrays.asList();
    }
    
    public static boolean isValidType(String type) {
        return TYPES.contains(type);
    }
    
    public static boolean isValidCategory(String type, String category) {
        return getCategories(type).contains(category);
    }
    
    private RecordConstant() {}
}
