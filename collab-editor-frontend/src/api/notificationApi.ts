import { api } from './request';

// 通知相关API
export const notificationApi = {
  getList: (userId: number, isRead?: boolean) => {
    const params = { userId, isRead };
    return api.get('/api/notification/list', { params }).then(res => res.data);
  },
  markAsRead: (notificationId: number) => {
    return api.put(`/api/notification/read/${notificationId}`).then(res => res.data);
  },
  markAllAsRead: (userId: number) => {
    const params = { userId };
    return api.put('/api/notification/read/all', {}, { params }).then(res => res.data);
  },
  getUnreadCount: (userId: number) => {
    const params = { userId };
    return api.get('/api/notification/unread/count', { params }).then(res => res.data);
  },
  delete: (notificationId: number) => {
    return api.delete(`/api/notification/${notificationId}`).then(res => res.data);
  }
};
