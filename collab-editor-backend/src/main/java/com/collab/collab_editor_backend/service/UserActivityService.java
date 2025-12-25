package com.collab.collab_editor_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.collab.collab_editor_backend.entity.UserActivity;
import com.collab.collab_editor_backend.util.Result;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户行为分析Service接口
 */
public interface UserActivityService {
    
    /**
     * 记录用户行为
     * @param userId 用户ID
     * @param activityType 行为类型
     * @param details 行为详情
     */
    void recordActivity(Long userId, String activityType, String details);
    
    /**
     * 记录用户行为（带对象信息）
     * @param userId 用户ID
     * @param activityType 行为类型
     * @param objectId 相关对象ID
     * @param objectType 相关对象类型
     * @param details 行为详情
     */
    void recordActivityWithObject(Long userId, String activityType, Long objectId, String objectType, String details);
    
    /**
     * 分页查询用户行为记录
     * @param userId 用户ID
     * @param activityType 行为类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param page 页码
     * @param pageSize 每页大小
     * @return 分页结果
     */
    Result<?> getUserActivityList(Long userId, String activityType, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer pageSize);
    
    /**
     * 获取用户行为统计
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    Result<?> getUserActivityStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取用户活跃天数统计
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃天数
     */
    Result<?> getUserActiveDays(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 获取用户最近的行为记录
     * @param userId 用户ID
     * @param limit 记录数量
     * @return 最近行为记录
     */
    Result<?> getRecentUserActivities(Long userId, Integer limit);
}
