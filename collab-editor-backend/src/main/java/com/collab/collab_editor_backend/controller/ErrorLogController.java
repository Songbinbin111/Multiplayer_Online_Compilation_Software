package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.entity.ErrorLog;
import com.collab.collab_editor_backend.service.ErrorLogService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 错误日志控制器
 */
@RestController
@RequestMapping("/api/error-logs")
public class ErrorLogController {
    
    @Autowired
    private ErrorLogService errorLogService;
    
    /**
     * 保存单条错误日志
     * @param errorLog 错误日志实体
     * @return 保存结果
     */
    @PostMapping
    public Result saveErrorLog(@RequestBody ErrorLog errorLog) {
        int result = errorLogService.saveErrorLog(errorLog);
        return result > 0 ? Result.success("保存成功") : Result.error("保存失败");
    }
    
    /**
     * 批量保存错误日志
     * @param errorLogs 错误日志列表
     * @return 保存结果
     */
    @PostMapping("/batch")
    public Result batchSaveErrorLogs(@RequestBody List<ErrorLog> errorLogs) {
        int result = errorLogService.batchSaveErrorLogs(errorLogs);
        return result > 0 ? Result.success("批量保存成功") : Result.error("批量保存失败");
    }
    
    /**
     * 查询所有错误日志
     * @return 错误日志列表
     */
    @GetMapping
    public Result getAllErrorLogs() {
        List<ErrorLog> errorLogs = errorLogService.getAllErrorLogs();
        return Result.success(errorLogs);
    }
    
    /**
     * 根据时间范围查询错误日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 错误日志列表
     */
    @GetMapping("/time-range")
    public Result getErrorLogsByTimeRange(@RequestParam String startTime, @RequestParam String endTime) {
        List<ErrorLog> errorLogs = errorLogService.getErrorLogsByTimeRange(startTime, endTime);
        return Result.success(errorLogs);
    }
    
    /**
     * 根据错误类型查询错误日志
     * @param type 错误类型
     * @return 错误日志列表
     */
    @GetMapping("/type")
    public Result getErrorLogsByType(@RequestParam String type) {
        List<ErrorLog> errorLogs = errorLogService.getErrorLogsByType(type);
        return Result.success(errorLogs);
    }
}
