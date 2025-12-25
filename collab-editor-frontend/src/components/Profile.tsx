import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../api/userApi';
import type { User } from '../api/userApi';
import ActivityAnalysis from './ActivityAnalysis';
import {
  Container,
  Paper,
  Box,
  Typography,
  Tabs,
  Tab,
  Avatar,
  Button,
  TextField,
  CircularProgress,
  Alert,
  IconButton,
  Divider,
  Stack,
  Grid
} from '@mui/material';
import PhotoCamera from '@mui/icons-material/PhotoCamera';
import {
  Edit as EditIcon,
} from '@mui/icons-material';
import SaveIcon from '@mui/icons-material/Save';
import CancelIcon from '@mui/icons-material/Cancel';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';

const Profile: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState<Partial<User>>({});
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarPreview, setAvatarPreview] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'profile' | 'activity'>('profile');
  const navigate = useNavigate();

  // 检查是否登录
  const checkLogin = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return false;
    }
    return true;
  };

  // 获取用户资料
  const fetchProfile = async () => {
    if (!checkLogin()) return;

    try {
      setLoading(true);
      // 从localStorage获取当前用户的userId
      const userId = parseInt(localStorage.getItem('userId') || '0');
      const response = await userApi.getProfile(userId);
      if (response.code === 200) {
        setUser(response.data);
        setEditForm(response.data);
        if (response.data.avatarUrl) {
          setAvatarPreview(response.data.avatarUrl);
        }
      } else {
        setError(response.message || '获取个人资料失败');
      }
    } catch (err: any) {
      setError(err.message || '获取个人资料失败，请检查网络');
    } finally {
      setLoading(false);
    }
  };

  // 组件挂载时获取用户资料
  useEffect(() => {
    fetchProfile();
  }, []);

  // 处理表单输入变化
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setEditForm(prev => ({ ...prev, [name]: value }));
  };

  // 处理头像文件选择
  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setAvatarFile(file);
      // 生成预览图
      const reader = new FileReader();
      reader.onloadend = () => {
        setAvatarPreview(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  // 保存用户信息
  const handleSaveProfile = async () => {
    try {
      setLoading(true);

      // 先上传头像（如果有新头像）
      if (avatarFile) {
        const avatarResponse = await userApi.uploadAvatar(avatarFile);
        if (avatarResponse.code === 200) {
          editForm.avatarUrl = avatarResponse.data;
        } else {
          setError(avatarResponse.message || '头像上传失败');
          return;
        }
      }

      // 更新用户信息
      const response = await userApi.updateProfile(editForm);
      if (response.code === 200) {
        // 更新本地状态
        if (user) {
          const updatedUser = { ...user, ...editForm } as User;
          setUser(updatedUser);
          setIsEditing(false);
          setError('');
          alert('个人资料更新成功');
        }
      } else {
        setError(response.message || '更新个人资料失败');
      }
    } catch (err: any) {
      setError(err.message || '更新个人资料失败，请检查网络');
    } finally {
      setLoading(false);
    }
  };

  // 取消编辑
  const handleCancelEdit = () => {
    setIsEditing(false);
    setEditForm(user || {});
    if (user?.avatarUrl) {
      setAvatarPreview(user.avatarUrl);
    }
    setAvatarFile(null);
  };

  if (loading && !user) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!user) {
    return (
      <Container maxWidth="sm" sx={{ mt: 8 }}>
        <Alert severity="error">{error || '用户信息不存在'}</Alert>
        <Button variant="contained" onClick={() => navigate('/login')} sx={{ mt: 2 }}>
          去登录
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ overflow: 'hidden' }}>
        <Box sx={{ p: 2, bgcolor: 'primary.main', color: 'primary.contrastText', display: 'flex', alignItems: 'center' }}>
          <IconButton color="inherit" onClick={() => navigate(-1)} sx={{ mr: 1 }}>
            <ArrowBackIcon />
          </IconButton>
          <Typography variant="h6">个人中心</Typography>
        </Box>

        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs
            value={activeTab}
            onChange={(_, newValue) => setActiveTab(newValue)}
            indicatorColor="primary"
            textColor="primary"
            centered
          >
            <Tab label="个人资料" value="profile" />
            <Tab label="行为分析" value="activity" />
          </Tabs>
        </Box>

        {error && <Alert severity="error" sx={{ m: 2 }}>{error}</Alert>}

        <Box sx={{ p: 3 }}>
          {activeTab === 'profile' ? (
            <Grid container spacing={4}>
              <Grid size={{ xs: 12, md: 4 }} sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Box sx={{ position: 'relative' }}>
                  <Avatar
                    src={avatarPreview}
                    alt={user.username}
                    sx={{ width: 150, height: 150, fontSize: 60, bgcolor: 'primary.light' }}
                  >
                    {!avatarPreview && user.username.charAt(0).toUpperCase()}
                  </Avatar>
                  <input
                    type="file"
                    accept="image/*"
                    style={{ display: 'none' }}
                    id="avatar-upload"
                    onChange={handleAvatarChange}
                    disabled={!isEditing}
                  />
                  {isEditing && (
                    <label htmlFor="avatar-upload">
                      <IconButton
                        component="span"
                        color="primary"
                        sx={{
                          position: 'absolute',
                          bottom: 0,
                          right: 0,
                          bgcolor: 'background.paper',
                          '&:hover': { bgcolor: 'grey.200' }
                        }}
                      >
                        <PhotoCamera />
                      </IconButton>
                    </label>
                  )}
                </Box>
                <Typography variant="h5" sx={{ mt: 2 }}>{user.nickname || user.username}</Typography>
                <Typography variant="body2" color="text.secondary">{user.role || '普通用户'}</Typography>
              </Grid>

              <Grid size={{ xs: 12, md: 8 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                  <Typography variant="h6">基本信息</Typography>
                  {!isEditing ? (
                    <Button startIcon={<EditIcon />} onClick={() => setIsEditing(true)}>
                      编辑资料
                    </Button>
                  ) : (
                    <Stack direction="row" spacing={1}>
                      <Button startIcon={<CancelIcon />} onClick={handleCancelEdit} color="inherit">
                        取消
                      </Button>
                      <Button startIcon={<SaveIcon />} variant="contained" onClick={handleSaveProfile} disabled={loading}>
                        保存
                      </Button>
                    </Stack>
                  )}
                </Box>
                <Divider sx={{ mb: 3 }} />

                <Grid container spacing={2}>
                  <Grid size={12}>
                    <TextField
                      label="用户名"
                      fullWidth
                      value={isEditing ? editForm.username : user.username}
                      disabled
                      variant={isEditing ? 'outlined' : 'filled'}
                    />
                  </Grid>
                  <Grid size={12}>
                    <TextField
                      label="昵称"
                      fullWidth
                      name="nickname"
                      value={isEditing ? (editForm.nickname || '') : (user.nickname || '')}
                      onChange={handleInputChange}
                      disabled={!isEditing}
                      variant={isEditing ? 'outlined' : 'filled'}
                      InputProps={{ readOnly: !isEditing }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      label="邮箱"
                      fullWidth
                      name="email"
                      value={isEditing ? (editForm.email || '') : (user.email || '')}
                      onChange={handleInputChange}
                      disabled={!isEditing}
                      variant={isEditing ? 'outlined' : 'filled'}
                      InputProps={{ readOnly: !isEditing }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      label="手机号"
                      fullWidth
                      name="phone"
                      value={isEditing ? (editForm.phone || '') : (user.phone || '')}
                      onChange={handleInputChange}
                      disabled={!isEditing}
                      variant={isEditing ? 'outlined' : 'filled'}
                      InputProps={{ readOnly: !isEditing }}
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      label="注册时间"
                      fullWidth
                      value={user.createTime ? new Date(user.createTime).toLocaleString() : '-'}
                      disabled
                      variant="filled"
                    />
                  </Grid>
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <TextField
                      label="最后更新"
                      fullWidth
                      value={user.updateTime ? new Date(user.updateTime).toLocaleString() : '-'}
                      disabled
                      variant="filled"
                    />
                  </Grid>
                </Grid>
              </Grid>
            </Grid>
          ) : (
            <ActivityAnalysis />
          )}
        </Box>
      </Paper>
    </Container>
  );
};

export default Profile;
