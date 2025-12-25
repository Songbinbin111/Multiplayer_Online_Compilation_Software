import React, { useState, useEffect } from 'react';
import {
  Badge,
  IconButton,
  Popover,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  ListItemButton,
  Typography,
  Box,
  Button,
  Divider,
  Avatar,
  Tooltip
} from '@mui/material';
import {
  Notifications as NotificationsIcon,
  Assignment as AssignmentIcon,
  ChatBubble as ChatBubbleIcon,
  DoneAll as DoneAllIcon
} from '@mui/icons-material';

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
  const [unreadCount, setUnreadCount] = useState(0);
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);

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

  // 打开通知面板
  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  // 关闭通知面板
  const handleClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);
  const id = open ? 'simple-popover' : undefined;

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
    <>
      <Tooltip title="通知">
        <IconButton onClick={handleClick} color="inherit">
          <Badge badgeContent={unreadCount} color="error">
            <NotificationsIcon />
          </Badge>
        </IconButton>
      </Tooltip>

      <Popover
        id={id}
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
          sx: { width: 360, maxHeight: 500 }
        }}
      >
        <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6" component="div">
            通知
          </Typography>
          {unreadCount > 0 && (
            <Button
              size="small"
              startIcon={<DoneAllIcon />}
              onClick={markAllAsRead}
            >
              全部已读
            </Button>
          )}
        </Box>
        <Divider />

        {notifications.length === 0 ? (
          <Box sx={{ p: 4, textAlign: 'center' }}>
            <Typography variant="body2" color="text.secondary">
              暂无通知
            </Typography>
          </Box>
        ) : (
          <List sx={{ width: '100%', bgcolor: 'background.paper', p: 0 }}>
            {notifications.map((notification, index) => (
              <React.Fragment key={notification.id}>
                {index > 0 && <Divider component="li" />}
                <ListItem
                  alignItems="flex-start"
                  disablePadding
                >
                  <ListItemButton
                    onClick={() => markAsRead(notification.id)}
                    sx={{
                      bgcolor: notification.isRead ? 'transparent' : 'action.hover',
                      '&:hover': { bgcolor: 'action.selected' }
                    }}
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: notification.type === 'mention' ? 'primary.main' : 'secondary.main' }}>
                        {notification.type === 'mention' ? <ChatBubbleIcon /> : <AssignmentIcon />}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                          <Typography variant="subtitle2" component="span">
                            {notification.title}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" component="span">
                            {formatTime(notification.createTime).split(' ')[1]}
                          </Typography>
                        </Box>
                      }
                      secondary={
                        <React.Fragment>
                          <Typography
                            sx={{ display: 'inline' }}
                            component="span"
                            variant="body2"
                            color="text.primary"
                          >
                            {notification.content}
                          </Typography>
                          <br />
                          <Typography variant="caption" color="text.secondary">
                            {formatTime(notification.createTime).split(' ')[0]}
                          </Typography>
                        </React.Fragment>
                      }
                    />
                  </ListItemButton>
                  {!notification.isRead && (
                    <Box
                      sx={{
                        width: 10,
                        height: 10,
                        borderRadius: '50%',
                        bgcolor: 'primary.main',
                        position: 'absolute',
                        right: 16,
                        top: '50%',
                        transform: 'translateY(-50%)',
                        pointerEvents: 'none'
                      }}
                    />
                  )}
                </ListItem>
              </React.Fragment>
            ))}
          </List>
        )}
      </Popover>
    </>
  );
};

export default Notification;
