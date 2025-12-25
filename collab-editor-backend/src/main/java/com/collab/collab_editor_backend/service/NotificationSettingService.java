package com.collab.collab_editor_backend.service;

import com.collab.collab_editor_backend.entity.NotificationSetting;

/**
 * 通知设置服务接口
 */
public interface NotificationSettingService {
    
    /**
     * 根据用户ID获取通知设置
     * @param userId 用户ID
     * @return 通知设置
     */
    NotificationSetting getByUserId(Long userId);
    
    /**
     * 更新通知设置
     * @param setting 通知设置
     * @return 更新后的通知设置
     */
    NotificationSetting update(NotificationSetting setting);
    
    /**
     * 初始化用户的通知设置（如果不存在）
     * @param userId 用户ID
     * @return 初始化的通知设置
     */
    NotificationSetting initializeSetting(Long userId);
}
