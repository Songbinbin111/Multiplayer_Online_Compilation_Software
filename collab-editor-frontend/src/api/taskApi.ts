import { api } from './request';

export interface Task {
  id: number;
  docId: number;
  title: string;
  content: string;
  creatorId: number;
  assigneeId: number;
  status: 0 | 1 | 2; // 0: 待处理, 1: 进行中, 2: 已完成
  createTime: string;
  updateTime: string;
  deadline?: string; // 截止日期
}

export const taskApi = {
  create: (data: { docId: number; title: string; content: string; assigneeId: number; deadline?: string }) => {
    return api.post('/api/task/create', data);
  },
  getByDocId: (docId: number) => {
    return api.get(`/api/task/list/${docId}`);
  },
  getMyAssigned: () => {
    return api.get('/api/task/my/assigned');
  },
  getMyCreated: () => {
    return api.get('/api/task/my/created');
  },
  updateStatus: (data: { taskId: number; status: number; deadline?: string }) => {
    return api.post('/api/task/update/status', data);
  },
  delete: (taskId: number) => {
    return api.delete(`/api/task/delete/${taskId}`);
  }
};