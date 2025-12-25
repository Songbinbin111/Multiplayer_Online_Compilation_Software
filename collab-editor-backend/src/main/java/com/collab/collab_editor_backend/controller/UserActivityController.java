package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.service.UserActivityService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 用户行为分析Controller
 */
@RestController
@RequestMapping("/api/activity")
public class UserActivityController {

    @Autowired
    private UserActivityService userActivityService;

    /**
     * 获取用户行为记录列表
     */
    @GetMapping("/list")
    public Result getUserActivityList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        return userActivityService.getUserActivityList(userId, activityType, startTime, endTime, page, pageSize);
    }

    /**
     * 获取用户行为统计
     */
    @GetMapping("/statistics")
    public Result getUserActivityStatistics(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        return userActivityService.getUserActivityStatistics(userId, startTime, endTime);
    }

    /**
     * 获取用户活跃天数
     */
    @GetMapping("/active-days")
    public Result getUserActiveDays(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        
        return userActivityService.getUserActiveDays(userId, startTime, endTime);
    }

    /**
     * 获取用户最近的行为记录
     */
    @GetMapping("/recent")
    public Result getRecentUserActivities(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        
        return userActivityService.getRecentUserActivities(userId, limit);
    }
}
