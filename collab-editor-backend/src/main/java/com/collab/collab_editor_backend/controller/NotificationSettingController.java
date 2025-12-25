package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.entity.NotificationSetting;
import com.collab.collab_editor_backend.service.NotificationSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 通知设置控制器
 */
@RestController
@RequestMapping("/api/notification/setting")
public class NotificationSettingController {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationSettingController.class);
    
    @Autowired
    private NotificationSettingService notificationSettingService;
    
    /**
     * 获取用户的通知设置
     * @param userId 用户ID
     * @return 通知设置
     */
    @GetMapping
    public NotificationSetting getSetting(@RequestParam Long userId) {
        logger.info("获取用户 {} 的通知设置", userId);
        return notificationSettingService.getByUserId(userId);
    }
    
    /**
     * 更新通知设置
     * @param setting 通知设置
     * @return 更新后的通知设置
     */
    @PutMapping
    public NotificationSetting updateSetting(@RequestBody NotificationSetting setting) {
        logger.info("更新用户 {} 的通知设置", setting.getUserId());
        return notificationSettingService.update(setting);
    }
}
