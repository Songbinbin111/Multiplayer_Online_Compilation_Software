import { api } from './request';

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
  },
  // 导入Word文档
  importWord: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/api/doc/import/word', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then(res => res.data);
  },
  // 导入PDF文档
  importPdf: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/api/doc/import/pdf', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then(res => res.data);
  },
  // 导出Word文档
  exportWord: (docId: number) => {
    return api.get(`/api/doc/export/word/${docId}`, {
      responseType: 'blob'
    });
  },
  // 导出PDF文档
  exportPdf: (docId: number, content: string) => {
    return api.post(`/api/doc/export/pdf/${docId}`, {
      content
    }, {
      responseType: 'blob'
    });
  }
};

// 文档版本相关API接口
export const versionApi = {
  // 获取文档版本列表
  getVersions: (docId: number) => api.get(`/api/version/list/${docId}`),

  // 获取指定版本内容
  getVersion: (versionId: number) => api.get(`/api/version/${versionId}`),

  // 创建版本
  createVersion: (docId: number, content: string, versionName?: string, description?: string) => {
    return api.post(`/api/version/create?docId=${docId}&content=${encodeURIComponent(content)}${versionName ? `&versionName=${encodeURIComponent(versionName)}` : ''}${description ? `&description=${encodeURIComponent(description)}` : ''}`);
  },

  // 回滚到指定版本
  rollbackToVersion: (docId: number, versionId: number) => {
    return api.post(`/api/version/rollback?docId=${docId}&versionId=${versionId}`);
  },

  // 删除版本
  deleteVersion: (versionId: number) => api.delete(`/api/version/${versionId}`)
};
