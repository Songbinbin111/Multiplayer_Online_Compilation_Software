import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 移除响应拦截器，让组件直接处理完整的axios响应

// 导出api实例供其他组件使用
export { api };

// 用户相关API
export const userApi = {
  register: (data: { username: string; password: string }) => {
    return api.post('/api/register', data).then(res => res.data);
  },
  login: (data: { username: string; password: string }) => {
    return api.post('/api/login', data).then(res => {
      localStorage.setItem('userId', res.data.userId);
      localStorage.setItem('username', res.data.username);
      return res.data;
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
    return api.get('/api/user/list').then(res => res.data);
  }
};

// 文档相关API
export const documentApi = {
  getList() {
    return api.get('/api/doc/list').then(res => res.data);
  },
  create: (title: string) => {
    return api.post('/api/doc/create', { title }).then(res => res.data);
  },
  getContent: (docId: number) => {
    return api.get(`/api/doc/content/${docId}`).then(res => res.data);
  },
  saveContent: (docId: number, content: string) => {
    return api.post('/api/doc/save', { docId, content }).then(res => res.data);
  }
};

// 任务相关API
export const taskApi = {
  create: (data: { docId: number; title: string; content: string; assigneeId: number }) => {
    return api.post('/api/task/create', data).then(res => res.data);
  },
  getByDocId: (docId: number) => {
    return api.get(`/api/task/list/${docId}`).then(res => res.data);
  },
  getMyAssigned: () => {
    return api.get('/api/task/my/assigned').then(res => res.data);
  },
  getMyCreated: () => {
    return api.get('/api/task/my/created').then(res => res.data);
  },
  updateStatus: (data: { taskId: number; status: number }) => {
    return api.post('/api/task/update/status', data).then(res => res.data);
  },
  delete: (taskId: number) => {
    return api.delete(`/api/task/delete/${taskId}`).then(res => res.data);
  }
};

// 评论相关API
export const commentApi = {
  create: (data: { docId: number; content: string; parentId?: number }) => {
    return api.post('/api/comment/create', data).then(res => res.data);
  },
  getByDocId: (docId: number) => {
    return api.get(`/api/comment/list/${docId}`).then(res => res.data);
  },
  getReplies: (parentId: number) => {
    return api.get(`/api/comment/replies/${parentId}`).then(res => res.data);
  },
  delete: (commentId: number) => {
    return api.delete(`/api/comment/delete/${commentId}`).then(res => res.data);
  }
};

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

// 文档权限相关API
export const permissionApi = {
  // 为用户分配文档权限
  assignPermission: (docId: number, userId: number, permissionType: number) => {
    return api.post(`/api/permission/assign/${docId}/${userId}/${permissionType}`).then(res => res.data);
  },

  // 移除用户的文档权限
  removePermission: (docId: number, userId: number) => {
    return api.delete(`/api/permission/remove/${docId}/${userId}`).then(res => res.data);
  },

  // 更新用户的文档权限
  updatePermission: (docId: number, userId: number, permissionType: number) => {
    return api.put(`/api/permission/update/${docId}/${userId}/${permissionType}`).then(res => res.data);
  },

  // 根据文档ID获取所有权限记录
  getPermissionsByDocId: (docId: number) => {
    return api.get(`/api/permission/list/${docId}`).then(res => res.data);
  },

  // 获取用户在指定文档的权限
  getPermissionByDocId: (docId: number) => {
    return api.get(`/api/permission/check/${docId}`).then(res => res.data);
  },

  // 检查用户是否有文档的查看权限
  checkViewPermission: (docId: number) => {
    return api.get(`/api/permission/check/view/${docId}`).then(res => res.data);
  },

  // 检查用户是否有文档的编辑权限
  checkEditPermission: (docId: number) => {
    return api.get(`/api/permission/check/edit/${docId}`).then(res => res.data);
  }
};

export default api;