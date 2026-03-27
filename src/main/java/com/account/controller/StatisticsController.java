package com.account.controller;

import com.account.common.BusinessException;
import com.account.common.Result;
import com.account.entity.User;
import com.account.service.StatisticsService;
import com.account.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统计控制器
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    private final UserService userService;
    
    public StatisticsController(StatisticsService statisticsService, UserService userService) {
        this.statisticsService = statisticsService;
        this.userService = userService;
    }
    
    /**
     * 按周统计
     * @param token 登录token
     * @param year 年份
     * @param week 周数
     * @return 统计结果
     */
    @GetMapping("/weekly")
    public Result<Map<String, Object>> weeklyStatistics(@RequestHeader(value = "Authorization", required = false) String token,
                                                         @RequestParam Integer year,
                                                         @RequestParam Integer week) {
        User user = checkLogin(token);
        Map<String, Object> result = statisticsService.weeklyStatistics(user.getId(), year, week);
        return Result.success(result);
    }
    
    /**
     * 按月统计
     * @param token 登录token
     * @param year 年份
     * @param month 月份
     * @return 统计结果
     */
    @GetMapping("/monthly")
    public Result<Map<String, Object>> monthlyStatistics(@RequestHeader(value = "Authorization", required = false) String token,
                                                          @RequestParam Integer year,
                                                          @RequestParam Integer month) {
        User user = checkLogin(token);
        Map<String, Object> result = statisticsService.monthlyStatistics(user.getId(), year, month);
        return Result.success(result);
    }
    
    /**
     * 按分类统计
     * @param token 登录token
     * @param type 类型（收入/支出）
     * @param year 年份（可选）
     * @param month 月份（可选）
     * @return 统计结果
     */
    @GetMapping("/category")
    public Result<Map<String, Object>> categoryStatistics(@RequestHeader(value = "Authorization", required = false) String token,
                                                           @RequestParam String type,
                                                           @RequestParam(required = false) Integer year,
                                                           @RequestParam(required = false) Integer month) {
        User user = checkLogin(token);
        Map<String, Object> result = statisticsService.categoryStatistics(user.getId(), type, year, month);
        return Result.success(result);
    }
    
    /**
     * 概览统计
     * @param token 登录token
     * @return 统计结果
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestHeader(value = "Authorization", required = false) String token) {
        User user = checkLogin(token);
        Map<String, Object> result = statisticsService.overview(user.getId());
        return Result.success(result);
    }
    
    private User checkLogin(String token) {
        User user = userService.getUserByToken(token);
        if (user == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return user;
    }
}
