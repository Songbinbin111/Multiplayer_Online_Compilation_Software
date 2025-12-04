import { api } from './request';

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
