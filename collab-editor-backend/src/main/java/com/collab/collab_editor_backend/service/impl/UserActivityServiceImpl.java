package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.collab.collab_editor_backend.entity.UserActivity;
import com.collab.collab_editor_backend.mapper.UserActivityMapper;
import com.collab.collab_editor_backend.service.UserActivityService;
import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户行为分析Service实现类
 */
@Service
public class UserActivityServiceImpl implements UserActivityService {

    @Autowired
    private UserActivityMapper userActivityMapper;

    /**
     * 记录用户行为
     */
    @Override
    public void recordActivity(Long userId, String activityType, String details) {
        recordActivityWithObject(userId, activityType, null, null, details);
    }

    /**
     * 记录用户行为（带对象信息）
     */
    @Override
    public void recordActivityWithObject(Long userId, String activityType, Long objectId, String objectType, String details) {
        UserActivity activity = new UserActivity();
        activity.setUserId(userId);
        activity.setActivityType(activityType);
        activity.setObjectId(objectId);
        activity.setObjectType(objectType);
        activity.setDetails(details);
        activity.setCreatedAt(LocalDateTime.now());
        
        userActivityMapper.insert(activity);
    }

    /**
     * 分页查询用户行为记录
     */
    @Override
    public Result<?> getUserActivityList(Long userId, String activityType, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer pageSize) {
        // 创建分页对象
        Page<UserActivity> pageParam = new Page<>(page, pageSize);
        
        // 查询数据
        IPage<UserActivity> activityPage = userActivityMapper.getUserActivities(pageParam, userId, activityType, startTime, endTime);
        
        // 封装结果
        Map<String, Object> data = new HashMap<>();
        data.put("records", activityPage.getRecords());
        data.put("total", activityPage.getTotal());
        data.put("current", activityPage.getCurrent());
        data.put("size", activityPage.getSize());
        data.put("pages", activityPage.getPages());
        
        return Result.successWithMessage("获取用户行为记录成功", data);
    }

    /**
     * 获取用户行为统计
     */
    @Override
    public Result<?> getUserActivityStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        // 获取行为类型分布
        List<Map<String, Object>> activityDistribution = userActivityMapper.countActivityTypeDistribution(userId, startTime, endTime);
        
        // 获取活跃天数
        Integer activeDays = userActivityMapper.countActiveDays(userId, startTime, endTime);
        
        // 封装结果
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("activityDistribution", activityDistribution);
        statistics.put("activeDays", activeDays);
        statistics.put("startTime", startTime);
        statistics.put("endTime", endTime);
        
        return Result.successWithMessage("获取用户行为统计成功", statistics);
    }

    /**
     * 获取用户活跃天数统计
     */
    @Override
    public Result<?> getUserActiveDays(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Integer activeDays = userActivityMapper.countActiveDays(userId, startTime, endTime);
        
        Map<String, Object> data = new HashMap<>();
        data.put("activeDays", activeDays);
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        
        return Result.successWithMessage("获取用户活跃天数成功", data);
    }

    /**
     * 获取用户最近的行为记录
     */
    @Override
    public Result<?> getRecentUserActivities(Long userId, Integer limit) {
        List<UserActivity> activities = userActivityMapper.getRecentActivities(userId, limit);
        return Result.successWithMessage("获取用户最近行为记录成功", activities);
    }
}
