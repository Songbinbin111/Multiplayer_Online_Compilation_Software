package com.collab.collab_editor_backend.service.impl;

import com.collab.collab_editor_backend.entity.ErrorLog;
import com.collab.collab_editor_backend.mapper.ErrorLogMapper;
import com.collab.collab_editor_backend.service.ErrorLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 错误日志服务实现类
 */
@Service
public class ErrorLogServiceImpl implements ErrorLogService {
    
    @Autowired
    private ErrorLogMapper errorLogMapper;
    
    @Override
    public int saveErrorLog(ErrorLog errorLog) {
        // 设置创建时间
        if (errorLog.getCreateTime() == null) {
            errorLog.setCreateTime(new Date());
        }

        // 设置发生时间
        if (errorLog.getTimestamp() == null) {
            errorLog.setTimestamp(errorLog.getCreateTime());
        }
        
        // 简单的报警机制：如果检测到严重错误（例如 GlobalError 或 UnhandledRejection），记录特殊日志
        // 在实际生产环境中，这里可以对接邮件、短信或钉钉/飞书机器人
        if ("GlobalError".equals(errorLog.getType()) || "UnhandledRejection".equals(errorLog.getType())) {
            System.err.println("【系统报警】检测到严重前端错误: " + errorLog.getMessage());
            // TODO: 发送邮件或消息通知管理员
            // notificationService.sendAlert("Admin", "Critical Error: " + errorLog.getMessage());
        }
        
        return errorLogMapper.insertErrorLog(errorLog);
    }
    
    @Override
    public int batchSaveErrorLogs(List<ErrorLog> errorLogs) {
        // 设置创建时间
        Date now = new Date();
        for (ErrorLog errorLog : errorLogs) {
            if (errorLog.getCreateTime() == null) {
                errorLog.setCreateTime(now);
            }
        }
        return errorLogMapper.batchInsertErrorLogs(errorLogs);
    }
    
    @Override
    public List<ErrorLog> getAllErrorLogs() {
        return errorLogMapper.selectAllErrorLogs();
    }
    
    @Override
    public List<ErrorLog> getErrorLogsByTimeRange(String startTime, String endTime) {
        return errorLogMapper.selectErrorLogsByTimeRange(startTime, endTime);
    }
    
    @Override
    public List<ErrorLog> getErrorLogsByType(String type) {
        return errorLogMapper.selectErrorLogsByType(type);
    }
    
    @Override
    public List<ErrorLog> getErrorLogsByUserId(Long userId) {
        return errorLogMapper.selectErrorLogsByUserId(userId);
    }
    
    @Override
    public List<ErrorLog> getErrorLogsByDocId(Long docId) {
        return errorLogMapper.selectErrorLogsByDocId(docId);
    }
}
