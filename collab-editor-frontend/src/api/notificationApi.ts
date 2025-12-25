import { api } from './request';

// 通知相关API
export const notificationApi = {
  getList: (userId: number, isRead?: boolean, type?: string) => {
    const params = { userId, isRead, type };
    return api.get('/api/notification/list', { params });
  },
  markAsRead: (notificationId: number) => {
    return api.put(`/api/notification/read/${notificationId}`);
  },
  markAllAsRead: (userId: number) => {
    const params = { userId };
    return api.put('/api/notification/read/all', {}, { params });
  },
  getUnreadCount: (userId: number) => {
    const params = { userId };
    return api.get('/api/notification/unread/count', { params });
  },
  delete: (notificationId: number) => {
    return api.delete(`/api/notification/${notificationId}`);
  }
};
