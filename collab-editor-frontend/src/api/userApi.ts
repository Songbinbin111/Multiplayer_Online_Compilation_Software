import { api } from './request';

// 用户相关API
export const userApi = {
  register: (data: { username: string; password: string }) => {
    return api.post('/api/register', data);
  },
  login: (data: { username: string; password: string }) => {
    return api.post('/api/login', data).then(res => {
      if (res.data && res.data.code === 200 && res.data.data) {
        localStorage.setItem('token', res.data.data.token);
        localStorage.setItem('userId', res.data.data.userId.toString());
        localStorage.setItem('username', res.data.data.username);
      }
      return res;
    });
  },
  logout: () => {
    // 前端退出，清除localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('userId');
    return Promise.resolve();
  },
  getList: () => {
    return api.get('/api/user/list');
  }
};
