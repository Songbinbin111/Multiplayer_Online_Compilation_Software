import { api } from './request';

export interface Task {
  id: string;
  content: string;
  docId: string;
  userId: string;
  status: 'pending' | 'completed';
  createTime: string;
  updateTime: string;
}

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