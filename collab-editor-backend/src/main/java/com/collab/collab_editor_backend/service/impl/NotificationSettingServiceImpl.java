package com.collab.collab_editor_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.collab.collab_editor_backend.entity.NotificationSetting;
import com.collab.collab_editor_backend.mapper.NotificationSettingMapper;
import com.collab.collab_editor_backend.service.NotificationSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * 通知设置服务实现类
 */
@Service
public class NotificationSettingServiceImpl extends ServiceImpl<NotificationSettingMapper, NotificationSetting> implements NotificationSettingService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationSettingServiceImpl.class);
    
    @Override
    public NotificationSetting getByUserId(Long userId) {
        logger.info("获取用户 {} 的通知设置", userId);
        try {
            NotificationSetting setting = baseMapper.getByUserId(userId);
            if (setting == null) {
                logger.info("用户 {} 无通知设置，将初始化默认设置", userId);
                return initializeSetting(userId);
            }
            return setting;
        } catch (Exception e) {
            logger.error("获取用户 {} 的通知设置失败", userId, e);
            throw e;
        }
    }
    
    @Override
    public NotificationSetting update(NotificationSetting setting) {
        logger.info("更新用户 {} 的通知设置", setting.getUserId());
        try {
            setting.setUpdateTime(LocalDateTime.now());
            updateById(setting);
            return setting;
        } catch (Exception e) {
            logger.error("更新用户 {} 的通知设置失败", setting.getUserId(), e);
            throw e;
        }
    }
    
    @Override
    public NotificationSetting initializeSetting(Long userId) {
        logger.info("初始化用户 {} 的通知设置", userId);
        try {
            NotificationSetting setting = new NotificationSetting();
            setting.setUserId(userId);
            setting.setMentionEnabled(true);
            setting.setTaskAssignEnabled(true);
            setting.setTaskStatusEnabled(true);
            setting.setEmailEnabled(false);
            setting.setCreateTime(LocalDateTime.now());
            setting.setUpdateTime(LocalDateTime.now());
            save(setting);
            return setting;
        } catch (Exception e) {
            logger.error("初始化用户 {} 的通知设置失败", userId, e);
            throw e;
        }
    }
}
