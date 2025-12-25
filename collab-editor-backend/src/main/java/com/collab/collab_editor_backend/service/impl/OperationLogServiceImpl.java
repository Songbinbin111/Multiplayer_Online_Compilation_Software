package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.collab_editor_backend.entity.OperationLog;
import com.collab.collab_editor_backend.mapper.OperationLogMapper;
import com.collab.collab_editor_backend.service.OperationLogService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现类
 */
@Service
public class OperationLogServiceImpl implements OperationLogService {
    
    @Autowired
    private OperationLogMapper operationLogMapper;
    
    @Override
    @Async // 异步执行，不影响主流程
    public void recordLog(Long userId, String username, String operationType, String operationContent, 
                         String ipAddress, String userAgent, Boolean success, String errorMessage) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setOperationType(operationType);
            log.setOperationContent(operationContent);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            log.setSuccess(success);
            log.setErrorMessage(errorMessage);
            log.setCreateTime(LocalDateTime.now());
            
            operationLogMapper.insert(log);
        } catch (Exception e) {
            // 记录日志失败时不抛出异常，避免影响主流程
            e.printStackTrace();
        }
    }
    
    @Override
    public Result<Map<String, Object>> getOperationLogs(int page, int pageSize, Map<String, Object> filters) {
        // 创建分页对象
        Page<OperationLog> logPage = new Page<>(page, pageSize);
        
        // 创建查询条件
        QueryWrapper<OperationLog> queryWrapper = new QueryWrapper<>();
        
        // 筛选条件
        if (filters != null) {
            // 按用户ID筛选
            if (filters.containsKey("userId")) {
                queryWrapper.eq("user_id", filters.get("userId"));
            }
            // 按用户名筛选
            if (filters.containsKey("username")) {
                queryWrapper.like("username", filters.get("username"));
            }
            // 按操作类型筛选
            if (filters.containsKey("operationType")) {
                queryWrapper.eq("operation_type", filters.get("operationType"));
            }
            // 按操作结果筛选
            if (filters.containsKey("success")) {
                queryWrapper.eq("success", filters.get("success"));
            }
        }
        
        // 默认按创建时间降序
        queryWrapper.orderByDesc("create_time");
        
        // 执行分页查询
        com.baomidou.mybatisplus.core.metadata.IPage<OperationLog> resultPage = operationLogMapper.selectPage(logPage, queryWrapper);
        
        // 构建返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("logs", resultPage.getRecords());
        response.put("total", resultPage.getTotal());
        response.put("pages", resultPage.getPages());
        response.put("current", resultPage.getCurrent());
        response.put("pageSize", resultPage.getSize());
        
        return Result.success(response);
    }
    
    @Override
    public Result<OperationLog> getOperationLogById(Long id) {
        OperationLog log = operationLogMapper.selectById(id);
        if (log == null) {
            return Result.error("操作日志不存在");
        }
        return Result.success(log);
    }
    
    @Override
    public Result<?> deleteOperationLog(Long id) {
        int result = operationLogMapper.deleteById(id);
        if (result > 0) {
            return Result.successWithMessage("操作日志删除成功");
        } else {
            return Result.error("操作日志不存在");
        }
    }
    
    @Override
    public Result<?> deleteBatchOperationLogs(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的日志");
        }
        
        int result = operationLogMapper.deleteBatchIds(ids);
        return Result.successWithMessage("成功删除" + result + "条操作日志");
    }
    
    @Override
    public Result<?> clearOperationLogs() {
        operationLogMapper.delete(null);
        return Result.successWithMessage("操作日志已清空");
    }
}
