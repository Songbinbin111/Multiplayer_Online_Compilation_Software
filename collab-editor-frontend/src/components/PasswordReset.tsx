import React, { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { userApi } from '../api/userApi';
import {
  Container,
  Box,
  Typography,
  TextField,
  Button,
  Paper,
  Alert,
  Link,
  Stack
} from '@mui/material';

const PasswordReset: React.FC = () => {
  const [identifier, setIdentifier] = useState(''); // 支持邮箱或手机号
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [step] = useState<'request' | 'reset'>(() => {
    const searchParams = new URLSearchParams(window.location.search);
    return searchParams.has('token') ? 'reset' : 'request';
  });

  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';

  // 请求密码重置
  const handleRequestReset = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');

    // 验证输入格式
    if (identifier.includes('@')) {
      // 邮箱格式验证
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(identifier)) {
        setError('邮箱格式不正确');
        return;
      }
    } else {
      // 手机号格式验证
      if (!/^1[3-9]\d{9}$/.test(identifier)) {
        setError('手机号格式不正确');
        return;
      }
    }

    try {
      const response = await userApi.requestPasswordReset(identifier);
      console.log('Password reset response:', response); // 调试日志
      if (response.code === 200) {
        // 处理响应数据，根据后端返回的实际结构
        let displayMessage = response.message || '操作成功';
        let resetToken = '';
        
        // 检查响应数据结构
        if (response.data && typeof response.data === 'object') {
          // 如果data中包含resetToken，则使用它
          resetToken = response.data.resetToken || '';
          // 如果data中包含message，则优先使用data中的message
          if (response.data.message) {
            displayMessage = response.data.message;
          }
        }
        
        setMessage(displayMessage);
        
        // 无论是否有token，都跳转到重置页面
        setTimeout(() => {
          if (resetToken) {
            navigate(`/reset-password?token=${resetToken}`);
          } else {
            navigate('/reset-password');
          }
        }, 1500);
      } else {
        setError(response.message || '请求失败');
      }
    } catch (err) {
      console.error('Password reset error:', err); // 调试日志
      setError('请求失败，请检查网络或联系方式');
    }
  };

  // 重置密码
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');

    if (newPassword !== confirmPassword) {
      setError('两次输入的密码不一致');
      return;
    }

    try {
      const response = await userApi.resetPassword(token, newPassword);
      if (response.code === 200) {
        setMessage('密码重置成功，3秒后跳转到登录页面');
        setTimeout(() => {
          navigate('/login');
        }, 3000);
      } else {
        setError(response.message || '重置失败');
      }
    } catch (err) {
      setError('重置失败，请检查网络或令牌是否有效');
    }
  };

  return (
    <Container component="main" maxWidth="xs">
      <Box
        sx={{
          marginTop: 8,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Paper elevation={3} sx={{ p: 4, width: '100%', borderRadius: 2 }}>
          <Typography component="h1" variant="h5" align="center" gutterBottom>
            {step === 'request' ? '密码找回' : '重置密码'}
          </Typography>
          
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {message && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}

          <Box component="form" onSubmit={step === 'request' ? handleRequestReset : handleResetPassword} sx={{ mt: 1 }}>
            {step === 'request' ? (
              <>
                <TextField
                  margin="normal"
                  required
                  fullWidth
                  id="identifier"
                  label="邮箱/手机号"
                  name="identifier"
                  autoComplete="email"
                  autoFocus
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  placeholder="请输入您的邮箱或手机号"
                />
                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  sx={{ mt: 3, mb: 2 }}
                >
                  发送重置链接
                </Button>
              </>
            ) : (
              <>
                <TextField
                  margin="normal"
                  required
                  fullWidth
                  name="newPassword"
                  label="新密码"
                  type="password"
                  id="newPassword"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
                <TextField
                  margin="normal"
                  required
                  fullWidth
                  name="confirmPassword"
                  label="确认新密码"
                  type="password"
                  id="confirmPassword"
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                />
                <Button
                  type="submit"
                  fullWidth
                  variant="contained"
                  sx={{ mt: 3, mb: 2 }}
                >
                  重置密码
                </Button>
              </>
            )}

            <Stack direction="row" justifyContent="center" spacing={2} sx={{ mt: 2 }}>
              {step === 'request' ? (
                <Link href="/login" variant="body2" underline="hover">
                  记得密码了？去登录
                </Link>
              ) : (
                <Link href="/reset-password" variant="body2" underline="hover">
                  重新请求重置链接
                </Link>
              )}
            </Stack>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default PasswordReset;
