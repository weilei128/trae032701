package com.account.controller;

import com.account.common.PageResult;
import com.account.common.Result;
import com.account.dto.AddRecordRequest;
import com.account.dto.QueryRecordRequest;
import com.account.dto.UpdateRecordRequest;
import com.account.entity.Record;
import com.account.entity.User;
import com.account.service.RecordService;
import com.account.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 记账控制器
 */
@RestController
@RequestMapping("/api/record")
public class RecordController {
    
    private final RecordService recordService;
    private final UserService userService;
    
    public RecordController(RecordService recordService, UserService userService) {
        this.recordService = recordService;
        this.userService = userService;
    }
    
    /**
     * 添加收支记录
     * @param token 登录token
     * @param request 添加请求
     * @return 记录信息
     */
    @PostMapping("/add")
    public Result<Map<String, Object>> addRecord(@RequestHeader(value = "Authorization", required = false) String token,
                                                  @Valid @RequestBody AddRecordRequest request) {
        User user = checkLogin(token);
        Record record = recordService.addRecord(user.getId(), request);
        return Result.success("添加成功", recordToMap(record));
    }
    
    /**
     * 更新收支记录
     * @param token 登录token
     * @param id 记录ID
     * @param request 更新请求
     * @return 记录信息
     */
    @PutMapping("/{id}")
    public Result<Map<String, Object>> updateRecord(@RequestHeader(value = "Authorization", required = false) String token,
                                                     @PathVariable String id,
                                                     @Valid @RequestBody UpdateRecordRequest request) {
        User user = checkLogin(token);
        Record record = recordService.updateRecord(user.getId(), id, request);
        return Result.success("更新成功", recordToMap(record));
    }
    
    /**
     * 删除收支记录
     * @param token 登录token
     * @param id 记录ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRecord(@RequestHeader(value = "Authorization", required = false) String token,
                                      @PathVariable String id) {
        User user = checkLogin(token);
        recordService.deleteRecord(user.getId(), id);
        return Result.success("删除成功", null);
    }
    
    /**
     * 查询收支记录
     * @param token 登录token
     * @param request 查询请求
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> queryRecords(@RequestHeader(value = "Authorization", required = false) String token,
                                                                 QueryRecordRequest request) {
        User user = checkLogin(token);
        PageResult<Record> pageResult = recordService.queryRecords(user.getId(), request);
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getPageNum());
        result.setPageSize(pageResult.getPageSize());
        result.setPages(pageResult.getPages());
        result.setList(pageResult.getList().stream()
                .map(this::recordToMap)
                .collect(java.util.stream.Collectors.toList()));
        
        return Result.success(result);
    }
    
    /**
     * 获取单条记录
     * @param token 登录token
     * @param id 记录ID
     * @return 记录信息
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getRecord(@RequestHeader(value = "Authorization", required = false) String token,
                                                  @PathVariable String id) {
        User user = checkLogin(token);
        Record record = recordService.getRecord(user.getId(), id);
        return Result.success(recordToMap(record));
    }
    
    private User checkLogin(String token) {
        User user = userService.getUserByToken(token);
        if (user == null) {
            throw new com.account.common.BusinessException(401, "用户未登录");
        }
        return user;
    }
    
    private Map<String, Object> recordToMap(Record record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.getId());
        map.put("amount", record.getAmount());
        map.put("type", record.getType());
        map.put("category", record.getCategory());
        map.put("remark", record.getRemark());
        map.put("createTime", record.getCreateTime());
        return map;
    }
}
