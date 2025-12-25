package com.collab.collab_editor_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.collab.collab_editor_backend.entity.Notification;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 通知模块Mapper接口
 */
public interface NotificationMapper extends BaseMapper<Notification> {
    
    /**
     * 根据用户ID获取所有通知
     * @param userId 用户ID
     * @param isRead 是否已读（可选，null表示所有通知）
     * @param type 通知类型（可选，null表示所有类型）
     * @return 通知列表
     */
    List<Notification> getNotificationsByUserId(Long userId, Boolean isRead, String type);
    
    /**
     * 将用户的所有通知标记为已读
     * @param userId 用户ID
     * @return 更新的记录数
     */
    int markAllAsRead(Long userId);
    
    /**
     * 获取用户未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    int countUnreadByUserId(Long userId);
}
