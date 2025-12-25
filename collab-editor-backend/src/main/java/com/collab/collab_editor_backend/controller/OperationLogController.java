package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.entity.OperationLog;
import com.collab.collab_editor_backend.service.OperationLogService;
import com.collab.collab_editor_backend.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 操作日志管理控制器
 */
@RestController
public class OperationLogController {
    
    private static final Logger logger = LoggerFactory.getLogger(OperationLogController.class);
    
    @Autowired
    private OperationLogService operationLogService;
    
    @GetMapping("/api/operation-logs")
    public Result<Map<String, Object>> getOperationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Boolean success) {
        logger.info("收到获取操作日志请求，页码: {}, 每页条数: {}", page, pageSize);
        
        // 构建筛选条件
        Map<String, Object> filters = new java.util.HashMap<>();
        if (userId != null) {
            filters.put("userId", userId);
        }
        if (username != null && !username.isEmpty()) {
            filters.put("username", username);
        }
        if (operationType != null && !operationType.isEmpty()) {
            filters.put("operationType", operationType);
        }
        if (success != null) {
            filters.put("success", success);
        }
        
        return operationLogService.getOperationLogs(page, pageSize, filters);
    }
    
    @GetMapping("/api/operation-logs/{id}")
    public Result<OperationLog> getOperationLogById(@PathVariable Long id) {
        logger.info("收到获取操作日志详情请求，日志ID: {}", id);
        return operationLogService.getOperationLogById(id);
    }
    
    @DeleteMapping("/api/operation-logs/{id}")
    public Result<?> deleteOperationLog(@PathVariable Long id) {
        logger.info("收到删除操作日志请求，日志ID: {}", id);
        return operationLogService.deleteOperationLog(id);
    }
    
    @DeleteMapping("/api/operation-logs")
    public Result<?> deleteBatchOperationLogs(@RequestBody List<Long> ids) {
        logger.info("收到批量删除操作日志请求，日志ID列表: {}", ids);
        return operationLogService.deleteBatchOperationLogs(ids);
    }
    
    @DeleteMapping("/api/operation-logs/clear")
    public Result<?> clearOperationLogs() {
        logger.info("收到清空操作日志请求");
        return operationLogService.clearOperationLogs();
    }
}
