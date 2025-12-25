package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.OperationLog;
import com.collab.collab_editor_backend.util.Result;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {
    /**
     * 记录操作日志
     */
    void recordLog(Long userId, String username, String operationType, String operationContent, 
                  String ipAddress, String userAgent, Boolean success, String errorMessage);
    
    /**
     * 获取操作日志列表（分页、筛选）
     */
    Result<Map<String, Object>> getOperationLogs(int page, int pageSize, Map<String, Object> filters);
    
    /**
     * 根据ID获取操作日志详情
     */
    Result<OperationLog> getOperationLogById(Long id);
    
    /**
     * 删除操作日志
     */
    Result<?> deleteOperationLog(Long id);
    
    /**
     * 批量删除操作日志
     */
    Result<?> deleteBatchOperationLogs(List<Long> ids);
    
    /**
     * 清空操作日志
     */
    Result<?> clearOperationLogs();
}
