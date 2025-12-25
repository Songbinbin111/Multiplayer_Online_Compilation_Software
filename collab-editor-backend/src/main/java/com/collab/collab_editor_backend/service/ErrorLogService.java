package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.ErrorLog;

import java.util.List;

/**
 * 错误日志服务接口
 */
public interface ErrorLogService {
    
    /**
     * 保存单条错误日志
     * @param errorLog 错误日志实体
     * @return 保存结果
     */
    int saveErrorLog(ErrorLog errorLog);
    
    /**
     * 批量保存错误日志
     * @param errorLogs 错误日志列表
     * @return 保存结果
     */
    int batchSaveErrorLogs(List<ErrorLog> errorLogs);
    
    /**
     * 查询所有错误日志
     * @return 错误日志列表
     */
    List<ErrorLog> getAllErrorLogs();
    
    /**
     * 根据时间范围查询错误日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 错误日志列表
     */
    List<ErrorLog> getErrorLogsByTimeRange(String startTime, String endTime);
    
    /**
     * 根据错误类型查询错误日志
     * @param type 错误类型
     * @return 错误日志列表
     */
    List<ErrorLog> getErrorLogsByType(String type);
    
    /**
     * 根据用户ID查询错误日志
     * @param userId 用户ID
     * @return 错误日志列表
     */
    List<ErrorLog> getErrorLogsByUserId(Long userId);
    
    /**
     * 根据文档ID查询错误日志
     * @param docId 文档ID
     * @return 错误日志列表
     */
    List<ErrorLog> getErrorLogsByDocId(Long docId);
}
