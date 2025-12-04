import React, { useState, useEffect } from 'react';
import { notificationApi } from '../api';

interface Notification {
  id: number;
  userId: number;
  type: string;
  content: string;
  docId: number;
  relatedId: number;
  isRead: boolean;
  createTime: string;
}

interface NotificationPanelProps {
  currentUserId: number;
}

const NotificationPanel: React.FC<NotificationPanelProps> = ({ currentUserId }) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [showPanel, setShowPanel] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  // 获取通知列表
  const fetchNotifications = async () => {
    try {
      const response = await notificationApi.getList(currentUserId);
      const data = response.data;
      setNotifications(data);
    } catch (error) {
      console.error('获取通知列表失败:', error);
    }
  };

  // 获取未读通知数量
  const fetchUnreadCount = async () => {
    try {
      const response = await notificationApi.getUnreadCount(currentUserId);
      const data = response.data;
      setUnreadCount(data);
    } catch (error) {
      console.error('获取未读通知数量失败:', error);
    }
  };

  // 标记通知为已读
  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await notificationApi.markAsRead(notificationId);
      // 更新本地通知状态
      setNotifications(prev =>
        prev.map(notification =>
          notification.id === notificationId
            ? { ...notification, isRead: true }
            : notification
        )
      );
      fetchUnreadCount();
    } catch (error) {
      console.error('标记通知为已读失败:', error);
    }
  };

  // 标记所有通知为已读
  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead(currentUserId);
      // 更新本地所有通知状态
      setNotifications(prev =>
        prev.map(notification => ({ ...notification, isRead: true }))
      );
      setUnreadCount(0);
    } catch (error) {
      console.error('标记所有通知为已读失败:', error);
    }
  };

  // 打开/关闭通知面板
  const togglePanel = () => {
    setShowPanel(!showPanel);
    // 打开面板时获取最新通知
    if (!showPanel) {
      fetchNotifications();
    }
  };

  // 获取通知类型文本
  const getNotificationTypeText = (type: string) => {
    switch (type) {
      case 'mention':
        return '@提及';
      case 'task_assign':
        return '任务分配';
      default:
        return '通知';
    }
  };

  // 获取通知类型样式类
  const getNotificationTypeClass = (type: string) => {
    switch (type) {
      case 'mention':
        return 'notification-type-mention';
      case 'task_assign':
        return 'notification-type-task';
      default:
        return 'notification-type-default';
    }
  };

  // 格式化时间
  const formatTime = (time: string) => {
    const date = new Date(time);
    return date.toLocaleString();
  };

  // 组件挂载时获取未读通知数量
  useEffect(() => {
    fetchUnreadCount();
  }, [currentUserId]);

  return (
    <div className="notification-container">
      {/* 通知按钮 */}
      <div className="notification-button" onClick={togglePanel}>
        <span className="notification-icon">📢</span>
        {unreadCount > 0 && (
          <span className="unread-count">{unreadCount}</span>
        )}
      </div>

      {/* 通知面板 */}
      {showPanel && (
        <div className="notification-panel">
          <div className="notification-panel-header">
            <h3>通知中心</h3>
            <button
              className="mark-all-read-btn"
              onClick={handleMarkAllAsRead}
              disabled={unreadCount === 0}
            >
              全部已读
            </button>
          </div>

          <div className="notification-list">
            {notifications.length === 0 ? (
              <p className="no-notifications">暂无通知</p>
            ) : (
              notifications.map(notification => (
                <div
                  key={notification.id}
                  className={`notification-item ${notification.isRead ? 'read' : 'unread'}`}
                  onClick={() => handleMarkAsRead(notification.id)}
                >
                  <div className="notification-type">
                    <span className={getNotificationTypeClass(notification.type)}>
                      {getNotificationTypeText(notification.type)}
                    </span>
                  </div>
                  <div className="notification-content">
                    {notification.content}
                  </div>
                  <div className="notification-time">
                    {formatTime(notification.createTime)}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationPanel;
