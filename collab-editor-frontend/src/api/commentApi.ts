import { api } from './request';

// 评论相关API
export const commentApi = {
  create: (data: { docId: number; content: string; parentId?: number; startPos?: number; endPos?: number; selectedText?: string }) => {
    return api.post('/api/comment/create', data);
  },
  getByDocId: (docId: number) => {
    return api.get(`/api/comment/list/${docId}`);
  },
  getReplies: (parentId: number) => {
    return api.get(`/api/comment/replies/${parentId}`);
  },
  delete: (commentId: number) => {
    return api.delete(`/api/comment/delete/${commentId}`);
  }
};
