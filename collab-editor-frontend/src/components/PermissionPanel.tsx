import React, { useState, useEffect } from 'react';
import { permissionApi, userApi } from '../api';
import {
  Box,
  Typography,
  Button,
  Select,
  MenuItem,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  FormControl,
  InputLabel,
  Paper,
  Chip,
  Snackbar,
  Alert
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import PersonAddIcon from '@mui/icons-material/PersonAdd';

// 定义权限类型
interface DocPermission {
  id: number;
  docId: number;
  userId: number;
  username?: string;
  permissionType: number;
  createTime: string;
}

// 定义用户类型
interface User {
  id: number;
  username: string;
}

// 定义组件属性
interface PermissionPanelProps {
  docId: number;
  currentUserId: number;
  onlineUsers: Array<{ userId: number; username: string }>;
}

const PermissionPanel: React.FC<PermissionPanelProps> = ({ docId, currentUserId, onlineUsers }) => {
  const [permissions, setPermissions] = useState<DocPermission[]>([]);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<number | null>(null);
  const [selectedPermission, setSelectedPermission] = useState<number>(0);
  const [isOwner, setIsOwner] = useState(false);
  const [openSnackbar, setOpenSnackbar] = useState(false);

  // 获取文档权限列表
  const fetchPermissions = async () => {
    try {
      const response = await permissionApi.getPermissionsByDocId(docId);
      let data = response.data || [];
      // 确保data是数组类型
      if (!Array.isArray(data)) {
        data = [];
      }
      setPermissions(data);
      // 检查当前用户是否是文档所有者
      const userPermission = data.find((p: DocPermission) => p.userId === currentUserId);
      const isAdminRole = localStorage.getItem('role') === 'admin';
      const hasAdminPermission = !!(userPermission && userPermission.permissionType === 2);
      setIsOwner(isAdminRole || hasAdminPermission);
    } catch (error) {
      console.error('获取权限列表失败:', error);
      setPermissions([]);
      setIsOwner(false);
    }
  };

  // 获取所有用户列表
  const fetchAllUsers = async () => {
    try {
      const response = await userApi.getList();
      const data = response.data || [];
      setAllUsers(data);
    } catch (error) {
      console.error('获取用户列表失败:', error);
      setAllUsers([]);
    }
  };

  // 分配权限
  const handleAssignPermission = async () => {
    if (!selectedUser) return;

    try {
      await permissionApi.assignPermission(docId, selectedUser, selectedPermission);
      fetchPermissions();
      setSelectedUser(null);
      // 保留当前选择的权限类型，方便继续分配
      setOpenSnackbar(true);
    } catch (error) {
      console.error('分配权限失败:', error);
    }
  };

  // 更新权限
  const handleUpdatePermission = async (userId: number, permissionType: number) => {
    try {
      await permissionApi.updatePermission(docId, userId, permissionType);
      fetchPermissions();
    } catch (error) {
      console.error('更新权限失败:', error);
    }
  };

  // 移除权限
  const handleRemovePermission = async (userId: number) => {
    if (window.confirm('确定要移除该用户的权限吗？')) {
      try {
        await permissionApi.removePermission(docId, userId);
        fetchPermissions();
      } catch (error) {
        console.error('移除权限失败:', error);
      }
    }
  };

  // 获取用户名
  const getUsername = (userId: number) => {
    const user = onlineUsers.find(u => u.userId === userId);
    if (user) return user.username;

    const allUser = allUsers.find(u => u.id === userId);
    if (allUser) return allUser.username;

    return '未知用户';
  };

  // 获取权限类型文本
  const getPermissionText = (type: number) => {
    if (type === 0) return '查看';
    if (type === 1) return '编辑';
    if (type === 2) return '管理员';
    return '未知';
  };

  // 检查用户是否已有权限
  const hasPermission = (userId: number) => {
    return permissions.some(p => p.userId === userId);
  };

  // 组件挂载时获取权限列表和用户列表
  useEffect(() => {
    fetchPermissions();
    fetchAllUsers();
  }, [docId]);

  const handleCloseSnackbar = () => {
    setOpenSnackbar(false);
  };

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6">文档权限</Typography>
      </Box>

      {/* 分配权限表单 */}
      {isOwner && (
        <Paper sx={{ p: 2, m: 2, bgcolor: 'background.paper', border: 1, borderColor: 'divider' }}>
          <Typography variant="subtitle2" sx={{ mb: 2, color: 'text.primary' }}>分配权限</Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <FormControl fullWidth size="small">
              <InputLabel sx={{ color: 'text.primary', '&.Mui-focused': { color: '#1976d2' } }}>选择用户</InputLabel>
              <Select
                value={selectedUser || ''}
                label="选择用户"
                onChange={(e) => setSelectedUser(Number(e.target.value))}
                sx={{
                  color: 'text.primary',
                  '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255, 255, 255, 0.23)' },
                  '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'text.primary' },
                  '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#1976d2' },
                  '.MuiSvgIcon-root': { color: 'text.primary' }
                }}
              >
                {allUsers.map(user => (
                  <MenuItem key={user.id} value={user.id} disabled={hasPermission(user.id)}>
                    {user.username}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth size="small">
              <InputLabel sx={{ color: 'text.primary', '&.Mui-focused': { color: '#1976d2' } }}>权限类型</InputLabel>
              <Select
                value={selectedPermission}
                label="权限类型"
                onChange={(e) => setSelectedPermission(Number(e.target.value))}
                sx={{
                  color: 'text.primary',
                  '.MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255, 255, 255, 0.23)' },
                  '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'text.primary' },
                  '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#1976d2' },
                  '.MuiSvgIcon-root': { color: 'text.primary' }
                }}
              >
                <MenuItem value={0}>查看权限</MenuItem>
                <MenuItem value={1}>编辑权限</MenuItem>
              </Select>
            </FormControl>
            <Button
              variant="contained"
              startIcon={<PersonAddIcon />}
              onClick={handleAssignPermission}
              disabled={!selectedUser}
              fullWidth
            >
              分配权限
            </Button>
          </Box>
        </Paper>
      )}

      {/* 权限列表 */}
      <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
        <Typography variant="subtitle2" sx={{ px: 2, pt: 2, pb: 1, color: 'text.secondary' }}>
          已授权用户
        </Typography>
        {permissions.length === 0 ? (
          <Box sx={{ p: 2, textAlign: 'center', color: 'text.secondary' }}>
            <Typography variant="body2">暂无权限设置</Typography>
          </Box>
        ) : (
          <List>
            {permissions.map(permission => (
              <ListItem key={permission.id} divider>
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center' }}>
                      <Typography variant="body1">{getUsername(permission.userId)}</Typography>
                      {permission.userId === currentUserId && (
                        <Chip label="我" size="small" color="primary" sx={{ ml: 1, height: 20 }} />
                      )}
                    </Box>
                  }
                  secondary={
                    isOwner && permission.userId !== currentUserId ? (
                      <FormControl variant="standard" size="small" sx={{ mt: 1, minWidth: 80 }}>
                        <Select
                          value={permission.permissionType}
                          onChange={(e) => handleUpdatePermission(permission.userId, Number(e.target.value))}
                          disableUnderline
                        >
                          <MenuItem value={0}>查看</MenuItem>
                          <MenuItem value={1}>编辑</MenuItem>
                          <MenuItem value={2}>管理员</MenuItem>
                        </Select>
                      </FormControl>
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        {getPermissionText(permission.permissionType)}
                      </Typography>
                    )
                  }
                />
                {isOwner && permission.userId !== currentUserId && (
                  <ListItemSecondaryAction>
                    <IconButton edge="end" aria-label="delete" onClick={() => handleRemovePermission(permission.userId)}>
                      <DeleteIcon color="error" />
                    </IconButton>
                  </ListItemSecondaryAction>
                )}
              </ListItem>
            ))}
          </List>
        )}
      </Box>

      {/* 成功提示 Snackbar */}
      <Snackbar
        open={openSnackbar}
        autoHideDuration={3000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={handleCloseSnackbar} severity="success" sx={{ width: '100%' }}>
          权限分配成功
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default PermissionPanel;
