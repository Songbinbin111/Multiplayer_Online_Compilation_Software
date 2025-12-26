import React, { useEffect, useState } from 'react';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Button,
  Select,
  MenuItem,
  IconButton,
  Typography,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Alert,
  Snackbar
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { userApi } from '../../api/userApi';
import type { User } from '../../api/userApi';

const UserManagement: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [_loading, setLoading] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<User | null>(null);
  const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await userApi.getList();
      // userApi.getList 返回的是 Result，数据在 data 字段，或者是直接返回数组（根据 api 实现）
      // 检查 userApi.ts 的 getList 实现: return api.get('/api/user/list').then(res => res.data);
      // UserController.java 的 getList 返回 Result.success(users)
      // 所以 res 是 Result 对象，res.data 是 user[]
      const userList = Array.isArray(res) ? res : (res.data || []);
      setUsers(userList);
    } catch (error) {
      console.error('获取用户列表失败:', error);
      setMessage({ type: 'error', text: '获取用户列表失败' });
    } finally {
      setLoading(false);
    }
  };

  const handleRoleChange = async (userId: number, newRole: string) => {
    try {
      await userApi.updateRole(userId, newRole);
      setMessage({ type: 'success', text: '角色更新成功' });
      fetchUsers();
    } catch (error) {
      console.error('更新角色失败:', error);
      setMessage({ type: 'error', text: '更新角色失败' });
    }
  };

  const handleDeleteClick = (user: User) => {
    setUserToDelete(user);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!userToDelete) return;
    try {
      await userApi.deleteUser(userToDelete.id);
      setMessage({ type: 'success', text: '用户删除成功' });
      fetchUsers();
    } catch (error) {
      console.error('删除用户失败:', error);
      setMessage({ type: 'error', text: '删除用户失败' });
    } finally {
      setDeleteDialogOpen(false);
      setUserToDelete(null);
    }
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>用户列表管理</Typography>
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>用户名</TableCell>
              <TableCell>昵称</TableCell>
              <TableCell>邮箱</TableCell>
              <TableCell>角色</TableCell>
              <TableCell>操作</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((user) => (
              <TableRow key={user.id}>
                <TableCell>{user.id}</TableCell>
                <TableCell>{user.username}</TableCell>
                <TableCell>{user.nickname}</TableCell>
                <TableCell>{user.email || '-'}</TableCell>
                <TableCell>
                  <Select
                    value={user.role || 'user'}
                    size="small"
                    onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    sx={{ minWidth: 100 }}
                  >
                    <MenuItem value="admin">管理员</MenuItem>
                    <MenuItem value="editor">编辑者</MenuItem>
                    <MenuItem value="viewer">观察者</MenuItem>
                  </Select>
                </TableCell>
                <TableCell>
                  <IconButton
                    color="error"
                    onClick={() => handleDeleteClick(user)}
                    disabled={user.role === 'admin'} // 防止删除管理员
                  >
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* 删除确认对话框 */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>确认删除用户</DialogTitle>
        <DialogContent>
          确定要删除用户 "{userToDelete?.username}" 吗？此操作无法撤销。
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>取消</Button>
          <Button onClick={handleDeleteConfirm} color="error" autoFocus>
            删除
          </Button>
        </DialogActions>
      </Dialog>

      {/* 消息提示 */}
      <Snackbar
        open={!!message}
        autoHideDuration={6000}
        onClose={() => setMessage(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={message?.type} onClose={() => setMessage(null)}>
          {message?.text}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default UserManagement;
