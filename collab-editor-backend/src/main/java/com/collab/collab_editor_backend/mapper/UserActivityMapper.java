package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.UserActivity;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户行为Mapper接口
 */
public interface UserActivityMapper extends BaseMapper<UserActivity> {
    
    /**
     * 分页查询用户行为记录
     */
    IPage<UserActivity> getUserActivities(IPage<UserActivity> page,
                                         @Param("userId") Long userId,
                                         @Param("activityType") String activityType,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户行为类型分布
     */
    List<Map<String, Object>> countActivityTypeDistribution(@Param("userId") Long userId,
                                                           @Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户活跃天数
     */
    Integer countActiveDays(@Param("userId") Long userId,
                           @Param("startTime") LocalDateTime startTime,
                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取用户最近的行为记录
     */
    List<UserActivity> getRecentActivities(@Param("userId") Long userId,
                                          @Param("limit") Integer limit);
}