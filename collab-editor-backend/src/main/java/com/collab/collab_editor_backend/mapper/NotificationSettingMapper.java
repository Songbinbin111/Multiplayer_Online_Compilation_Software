package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.NotificationSetting;
import org.apache.ibatis.annotations.Select;

/**
 * 通知设置模块Mapper接口
 */
public interface NotificationSettingMapper extends BaseMapper<NotificationSetting> {
    
    /**
     * 根据用户ID获取通知设置
     * @param userId 用户ID
     * @return 通知设置
     */
    @Select("SELECT * FROM t_notification_setting WHERE user_id = #{userId}")
    NotificationSetting getByUserId(Long userId);
}
