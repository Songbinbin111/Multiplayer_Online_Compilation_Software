import React, { useState, useEffect } from 'react';

// 通知接口定义
interface NotificationItem {
  id: number;
  type: string; // mention 或 task
  title: string;
  content: string;
  createTime: string;
  isRead: boolean;
}

interface NotificationProps {
  // 可以添加其他属性，如通知数据源等
}

const Notification: React.FC<NotificationProps> = () => {
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  // 模拟获取通知数据
  const fetchNotifications = async () => {
    // 这里应该从后端API获取通知数据
    // 目前使用模拟数据
    const mockNotifications: NotificationItem[] = [
      {
        id: 1,
        type: 'mention',
        title: '有人提到了你',
        content: '用户user1在文档中@了你',
        createTime: new Date().toISOString(),
        isRead: false
      },
      {
        id: 2,
        type: 'task',
        title: '新任务分配',
        content: '用户admin为你分配了一个新任务',
        createTime: new Date(Date.now() - 3600000).toISOString(),
        isRead: false
      }
    ];
    setNotifications(mockNotifications);
    setUnreadCount(mockNotifications.filter(n => !n.isRead).length);
  };

  // 标记通知为已读
  const markAsRead = (id: number) => {
    setNotifications(prev => 
      prev.map(notification => 
        notification.id === id ? { ...notification, isRead: true } : notification
      )
    );
    setUnreadCount(prev => Math.max(prev - 1, 0));
  };

  // 标记所有通知为已读
  const markAllAsRead = () => {
    setNotifications(prev => 
      prev.map(notification => ({ ...notification, isRead: true }))
    );
    setUnreadCount(0);
  };

  // 格式化时间
  const formatTime = (timeString: string) => {
    const date = new Date(timeString);
    return date.toLocaleString();
  };

  // 组件挂载时获取通知
  useEffect(() => {
    fetchNotifications();
    // 定时刷新通知
    const interval = setInterval(fetchNotifications, 30000); // 30秒刷新一次
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="notification-container">
      {/* 通知按钮 */}
      <button 
        className="notification-btn"
        onClick={() => setShowNotifications(!showNotifications)}
      >
        <span className="notification-icon">🔔</span>
        {unreadCount > 0 && (
          <span className="notification-badge">{unreadCount}</span>
        )}
      </button>

      {/* 通知面板 */}
      {showNotifications && (
        <div className="notification-panel">
          <div className="notification-panel-header">
            <h3>通知</h3>
            {unreadCount > 0 && (
              <button 
                className="mark-all-read-btn"
                onClick={markAllAsRead}
              >
                全部已读
              </button>
            )}
          </div>
          <div className="notification-list">
            {notifications.length === 0 ? (
              <p className="no-notifications">暂无通知</p>
            ) : (
              notifications.map(notification => (
                <div 
                  key={notification.id} 
                  className={`notification-item ${notification.isRead ? 'read' : 'unread'}`}
                  onClick={() => markAsRead(notification.id)}
                >
                  <div className="notification-type">
                    {notification.type === 'mention' ? '💬' : '📋'}
                  </div>
                  <div className="notification-content">
                    <div className="notification-title">{notification.title}</div>
                    <div className="notification-text">{notification.content}</div>
                    <div className="notification-time">{formatTime(notification.createTime)}</div>
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

export default Notification;
