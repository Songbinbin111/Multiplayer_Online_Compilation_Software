import { api } from './request';

// 文档相关API
export const documentApi = {
  // 获取文档列表
  getList() {
    return api.get('/api/doc/list');
  },
  // 按分类获取文档列表
  getListByCategory: (category: string) => {
    return api.get(`/api/doc/list/category/${category}`);
  },
  // 获取所有分类
  getCategories: () => {
    console.log('documentApi.getCategories called');
    return api.get('/api/doc/categories');
  },
  // 搜索文档（支持分类）
  search: (keyword?: string, startDate?: string, endDate?: string, sortField?: string, sortOrder?: string, category?: string, scope?: 'title' | 'title_exact' | 'content' | 'all') => {
    return api.get('/api/doc/search', { params: { keyword, startDate, endDate, sortField, sortOrder, category, scope } });
  },
  // 创建文档
  create: (title: string, category?: string, tags?: string, content?: string) => {
    return api.post('/api/doc/create', { title, category, tags, content });
  },
  // 删除文档
  delete: (docId: number) => {
    return api.delete(`/api/doc/${docId}`);
  },
  // 获取文档内容
  getContent: (docId: number) => {
    return api.get(`/api/doc/content/${docId}`);
  },
  // 保存文档内容
  saveContent: (docId: number, content: string) => {
    return api.post('/api/doc/save', { docId, content });
  },
  // 导入Word文档
  importWord: (file: File, category?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (category) formData.append('category', category);
    return api.post('/api/doc/import/word', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
  },
  // 导入PDF文档
  importPdf: (file: File, category?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    if (category) formData.append('category', category);
    return api.post('/api/doc/import/pdf', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
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
  },
  // 导出文档
  exportDocument: (docId: number, type: 'pdf' | 'markdown' | 'word') => {
    return api.get(`/api/doc/export/${type}/${docId}`, {
      responseType: 'blob'
    });
  },
  // 恢复版本
  restoreVersion: (versionId: number) => {
    // 复用 versionApi 的回滚功能
    return api.post(`/api/version/rollback?versionId=${versionId}`);
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
    return api.post('/api/version/create', null, {
      params: { docId, content, versionName, description }
    });
  },

  // 回滚到指定版本
  rollbackToVersion: (docId: number, versionId: number) => {
    return api.post('/api/version/rollback', null, {
      params: { docId, versionId }
    });
  },

  // 删除版本
  deleteVersion: (versionId: number) => api.delete(`/api/version/${versionId}`),

  // 获取版本差异
  getVersionDiff: (versionId1: number, versionId2: number) => api.get('/api/version/diff', { params: { versionId1, versionId2 } }),

  // 锁定/解锁版本
  lockVersion: (versionId: number, isLocked: boolean) => {
    return api.post('/api/version/lock', null, {
      params: { versionId, isLocked }
    });
  }
};
