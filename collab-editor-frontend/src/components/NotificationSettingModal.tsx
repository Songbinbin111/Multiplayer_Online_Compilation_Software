import React, { useState, useEffect } from 'react';
import { notificationSettingApi } from '../api';
import type { NotificationSetting } from '../api';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Switch,
  Typography,
  CircularProgress,
  IconButton,
  Divider,
  Alert
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

interface NotificationSettingModalProps {
  isOpen: boolean;
  onClose: () => void;
  userId: number;
}

const NotificationSettingModal: React.FC<NotificationSettingModalProps> = ({ isOpen, onClose, userId }) => {
  const [setting, setSetting] = useState<NotificationSetting>({
    id: 0,
    userId,
    mentionEnabled: true,
    taskAssignEnabled: true,
    taskStatusEnabled: true,
    emailEnabled: false,
    createTime: '',
    updateTime: ''
  });

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // 加载通知设置
  useEffect(() => {
    if (isOpen && userId) {
      fetchSetting();
    }
  }, [isOpen, userId]);

  const fetchSetting = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await notificationSettingApi.getSetting(userId);
      const data = response.data;
      setSetting(data);
    } catch (error) {
      console.error('获取通知设置失败:', error);
      setError('获取通知设置失败，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  // 更新设置
  const handleSettingChange = (key: keyof NotificationSetting, value: boolean) => {
    setSetting(prev => ({
      ...prev,
      [key]: value
    }));
  };

  // 保存设置
  const handleSave = async () => {
    try {
      setSaving(true);
      setError(null);
      await notificationSettingApi.updateSetting(setting);
      onClose();
    } catch (error) {
      console.error('保存通知设置失败:', error);
      setError('保存设置失败，请稍后重试');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog 
      open={isOpen} 
      onClose={onClose}
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle sx={{ m: 0, p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6" component="div">通知设置</Typography>
        <IconButton
          aria-label="close"
          onClick={onClose}
          sx={{
            color: (theme) => theme.palette.grey[500],
          }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      
      <DialogContent dividers>
        {loading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: '20px' }}>
            <CircularProgress />
          </div>
        ) : (
          <>
            {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
            
            <List sx={{ width: '100%', bgcolor: 'background.paper' }}>
              <ListItem>
                <ListItemText 
                  primary="@提及通知" 
                  secondary="当有人在评论或聊天中@你时接收通知" 
                />
                <ListItemSecondaryAction>
                  <Switch
                    edge="end"
                    checked={setting.mentionEnabled}
                    onChange={(e) => handleSettingChange('mentionEnabled', e.target.checked)}
                  />
                </ListItemSecondaryAction>
              </ListItem>
              
              <Divider component="li" />
              
              <ListItem>
                <ListItemText 
                  primary="任务分配通知" 
                  secondary="当有人分配任务给你时接收通知" 
                />
                <ListItemSecondaryAction>
                  <Switch
                    edge="end"
                    checked={setting.taskAssignEnabled}
                    onChange={(e) => handleSettingChange('taskAssignEnabled', e.target.checked)}
                  />
                </ListItemSecondaryAction>
              </ListItem>
              
              <Divider component="li" />
              
              <ListItem>
                <ListItemText 
                  primary="任务状态变更通知" 
                  secondary="当你参与的任务状态变更时接收通知" 
                />
                <ListItemSecondaryAction>
                  <Switch
                    edge="end"
                    checked={setting.taskStatusEnabled}
                    onChange={(e) => handleSettingChange('taskStatusEnabled', e.target.checked)}
                  />
                </ListItemSecondaryAction>
              </ListItem>
              
              <Divider component="li" />
              
              <ListItem>
                <ListItemText 
                  primary="邮件通知" 
                  secondary="重要通知同时发送邮件" 
                />
                <ListItemSecondaryAction>
                  <Switch
                    edge="end"
                    checked={setting.emailEnabled}
                    onChange={(e) => handleSettingChange('emailEnabled', e.target.checked)}
                  />
                </ListItemSecondaryAction>
              </ListItem>
            </List>
          </>
        )}
      </DialogContent>
      
      <DialogActions>
        <Button onClick={onClose} color="inherit">
          取消
        </Button>
        <Button onClick={handleSave} variant="contained" disabled={saving}>
          {saving ? '保存中...' : '保存'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default NotificationSettingModal;
