import { api } from './request';

// 用户信息接口定义
export interface User {
  id: number;
  username: string;
  password?: string;
  nickname?: string;
  avatarUrl?: string;
  email?: string;
  phone?: string;
  role?: string;
  createTime?: string;
  updateTime?: string;
}

// 用户相关API
export const userApi = {
  register: (data: { username?: string; email?: string; phone?: string; password: string }) => {
    return api.post('/api/register', data).then(res => res.data);
  },
  login: (data: { username: string; password: string }) => {
    return api.post('/api/login', data).then(res => {
      if (res.data.code === 200 && res.data.data) {
        localStorage.setItem('token', res.data.data.token);
        localStorage.setItem('userId', res.data.data.userId.toString());
        localStorage.setItem('username', res.data.data.username);
        localStorage.setItem('role', res.data.data.role || 'user');
      }
      return res.data;
    });
  },
  logout: () => {
    // 前端退出，清除localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('userId');
    localStorage.removeItem('role');
    return Promise.resolve();
  },
  getList: () => {
    return api.get('/api/user/list').then(res => res.data);
  },
  // 获取个人资料
  getProfile: (userId: number) => {
    return api.get('/api/user/profile', { params: { userId } }).then(res => res.data);
  },
  // 更新用户信息
  updateProfile: (data: Partial<User>) => {
    return api.post('/api/user/update', data).then(res => res.data);
  },
  // 上传头像
  uploadAvatar: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/api/user/avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then(res => res.data);
  },
  // 请求密码重置
  requestPasswordReset: (identifier: string) => {
    return api.post('/api/user/reset-password/request', { identifier }).then(res => res.data);
  },
  // 重置密码
  resetPassword: (token: string, newPassword: string) => {
    return api.post('/api/user/reset-password', { token, newPassword }).then(res => res.data);
  },
  // 更新用户角色
  updateRole: (userId: number, newRole: string) => {
    return api.post('/api/user/update-role', null, { params: { userId, newRole } }).then(res => res.data);
  },
  // 删除用户
  deleteUser: (userId: number) => {
    return api.post('/api/user/delete', null, { params: { userId } }).then(res => res.data);
  }
};
