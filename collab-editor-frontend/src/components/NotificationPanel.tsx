import React, { useState, useEffect, useRef } from 'react';
import { notificationApi } from '../api';
import {
  Badge,
  IconButton,
  Typography,
  Box,
  Button,
  Tabs,
  Tab,
  Popover,
  List,
  ListItemText,
  ListItemButton,
  ListItemIcon
} from '@mui/material';
import {
  Notifications as NotificationsIcon,
  Check as CheckIcon,
  Delete as DeleteIcon,
  FiberManualRecord as DotIcon,
  DoneAll as DoneAllIcon,
  Assignment as TaskIcon,
  Comment as CommentIcon,
  AlternateEmail as MentionIcon,
  Group as CollabIcon,
  InsertDriveFile as FileIcon,
  Info as InfoIcon
} from '@mui/icons-material';

// 定义Notification接口
interface Notification {
  id: number;
  userId: number;
  type: string;
  content: string;
  isRead: boolean;
  createTime: string;
  readTime?: string;
}

// 定义NotificationType类型
type NotificationType = 'mention' | 'comment' | 'reply' | 'task' | 'collab' | 'file' | 'all';

interface NotificationPanelProps {
  currentUserId: number;
}

const NotificationPanel: React.FC<NotificationPanelProps> = ({ currentUserId }) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [filterType, setFilterType] = useState<NotificationType>('all');
  const filterTypeRef = useRef<NotificationType>('all');
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  const handleClick = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);

  useEffect(() => {
    filterTypeRef.current = filterType;
  }, [filterType]);

  // 获取通知列表
  const fetchNotifications = async () => {
    try {
      const response = await notificationApi.getList(currentUserId);
      // 确保filteredNotifications是数组
      const notificationsData = response.data && response.data.data ? response.data.data : [];
      let filteredNotifications = Array.isArray(notificationsData) ? notificationsData : [];

      // 根据过滤类型筛选通知
      if (filterType !== 'all') {
        filteredNotifications = filteredNotifications.filter((notification: Notification) => {
          if (filterType === 'task') {
            return ['task', 'task_assign', 'task_status'].includes(notification.type);
          }
          return notification.type === filterType;
        });
      }

      setNotifications(filteredNotifications);
      setUnreadCount(filteredNotifications.filter((notification: Notification) => !notification.isRead).length);
    } catch (error) {
      console.error('获取通知失败:', error);
      // alert('获取通知失败');
    }
  };

  // 标记通知为已读
  const handleMarkAsRead = async (notificationId: number) => {
    try {
      await notificationApi.markAsRead(notificationId);
      setNotifications(prev => prev.map(n =>
        n.id === notificationId ? { ...n, isRead: true } : n
      ));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error('标记通知为已读失败:', error);
      // alert('标记通知为已读失败');
    }
  };

  // 标记所有通知为已读
  const handleMarkAllAsRead = async () => {
    try {
      await notificationApi.markAllAsRead(currentUserId);
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('标记所有通知为已读失败:', error);
      // alert('标记所有通知为已读失败');
    }
  };

  // 删除通知
  const handleDeleteNotification = async (notificationId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationApi.delete(notificationId);
      await fetchNotifications();
    } catch (error) {
      console.error('删除通知失败:', error);
      // alert('删除通知失败');
    }
  };

  // 格式化时间
  const formatTime = (timeStr: string) => {
    const date = new Date(timeStr);
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  // 获取通知类型图标
  const getNotificationIcon = (type: string) => {
    switch (type) {
      case 'mention': return <MentionIcon color="primary" fontSize="small" />;
      case 'comment': return <CommentIcon color="success" fontSize="small" />;
      case 'reply': return <CommentIcon color="info" fontSize="small" />;
      case 'task':
      case 'task_assign':
      case 'task_status': return <TaskIcon color="warning" fontSize="small" />;
      case 'collab': return <CollabIcon color="secondary" fontSize="small" />;
      case 'file': return <FileIcon color="action" fontSize="small" />;
      default: return <InfoIcon color="disabled" fontSize="small" />;
    }
  };

  // 处理通知点击
  const handleNotificationClick = (notification: Notification) => {
    if (!notification.isRead) {
      handleMarkAsRead(notification.id);
    }
    // 这里可以添加通知点击后的跳转逻辑
  };

  // 组件挂载时获取通知并建立WebSocket连接
  useEffect(() => {
    if (currentUserId) {
      fetchNotifications();
    }
  }, [currentUserId, filterType]);

  useEffect(() => {
    if (!currentUserId) return;

    // 建立WebSocket连接
    const apiUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
    const apiUrlStr = String(apiUrl);
    const wsProtocol = apiUrlStr.startsWith('https') ? 'wss' : 'ws';
    const baseUrl = apiUrlStr.replace(/^https?:\/\//, '');
    const wsUrl = `${wsProtocol}://${baseUrl}/ws/chat?userId=${currentUserId}`;

    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('通知WebSocket连接已建立');
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        if (message.type === 'new_message') return;

        if (message.id && message.type && message.content) {
          fetchNotifications();

          if (Notification.permission === 'granted' && !document.hasFocus()) {
            new Notification('新通知', { body: message.content });
          }
        }
      } catch (error) {
        console.error('解析WebSocket消息失败:', error);
      }
    };

    return () => {
      ws.close();
    };
  }, [currentUserId]);

  return (
    <>
      <IconButton color="inherit" onClick={handleClick}>
        <Badge badgeContent={unreadCount} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>

      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        PaperProps={{
          sx: { width: 360, maxHeight: 500, display: 'flex', flexDirection: 'column' }
        }}
      >
        <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: 1, borderColor: 'divider' }}>
          <Typography variant="subtitle1" fontWeight="bold">通知中心</Typography>
          {unreadCount > 0 && (
            <Button size="small" onClick={handleMarkAllAsRead} startIcon={<DoneAllIcon />} sx={{ textTransform: 'none' }}>
              全部已读
            </Button>
          )}
        </Box>

        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs
            value={filterType}
            onChange={(_, newValue) => setFilterType(newValue)}
            variant="scrollable"
            scrollButtons="auto"
            textColor="primary"
            indicatorColor="primary"
            sx={{ minHeight: 48, '& .MuiTab-root': { minWidth: 60, fontSize: '0.8rem' } }}
          >
            <Tab label="全部" value="all" />
            <Tab label="提及" value="mention" />
            <Tab label="评论" value="comment" />
            <Tab label="任务" value="task" />
          </Tabs>
        </Box>

        <List sx={{ overflow: 'auto', flexGrow: 1, p: 0 }}>
          {notifications.length === 0 ? (
            <Box sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>
              <Typography variant="body2">暂无通知</Typography>
            </Box>
          ) : (
            notifications.map((notification) => (
              <ListItemButton
                key={notification.id}
                onClick={() => handleNotificationClick(notification)}
                sx={{
                  bgcolor: notification.isRead ? 'transparent' : 'action.hover',
                  borderBottom: '1px solid',
                  borderColor: 'divider',
                  alignItems: 'flex-start',
                  pr: 6 // space for actions
                }}
              >
                <ListItemIcon sx={{ minWidth: 36, mt: 0.5 }}>
                  {getNotificationIcon(notification.type)}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
                      {!notification.isRead && <DotIcon color="error" sx={{ fontSize: 8, mr: 1 }} />}
                      <Typography variant="body2" sx={{ fontWeight: notification.isRead ? 'normal' : 'bold' }}>
                        {notification.content}
                      </Typography>
                    </Box>
                  }
                  secondary={formatTime(notification.createTime)}
                />
                <Box sx={{ position: 'absolute', right: 8, top: 8, display: 'flex', flexDirection: 'column', gap: 0.5 }}>
                  {!notification.isRead && (
                    <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleMarkAsRead(notification.id); }} title="标记为已读">
                      <CheckIcon fontSize="small" />
                    </IconButton>
                  )}
                  <IconButton size="small" onClick={(e) => { e.stopPropagation(); handleDeleteNotification(notification.id, e); }} title="删除">
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Box>
              </ListItemButton>
            ))
          )}
        </List>
      </Popover>
    </>
  );
};

export default NotificationPanel;