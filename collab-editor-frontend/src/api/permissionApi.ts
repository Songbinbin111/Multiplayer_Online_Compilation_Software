import { api } from './request';

// 文档权限相关API
export const permissionApi = {
  // 为用户分配文档权限
  assignPermission: (docId: number, userId: number, permissionType: number) => {
    return api.post(`/api/permission/assign/${docId}/${userId}/${permissionType}`);
  },

  // 移除用户的文档权限
  removePermission: (docId: number, userId: number) => {
    return api.delete(`/api/permission/remove/${docId}/${userId}`);
  },

  // 更新用户的文档权限
  updatePermission: (docId: number, userId: number, permissionType: number) => {
    return api.put(`/api/permission/update/${docId}/${userId}/${permissionType}`);
  },

  // 根据文档ID获取所有权限记录
  getPermissionsByDocId: (docId: number) => {
    return api.get(`/api/permission/list/${docId}`);
  },

  // 获取用户在指定文档的权限
  getPermissionByDocId: (docId: number) => {
    return api.get(`/api/permission/check/${docId}`);
  },

  // 检查用户是否有文档的查看权限
  checkViewPermission: (docId: number) => {
    return api.get(`/api/permission/check/view/${docId}`);
  },

  // 检查用户是否有文档的编辑权限
  checkEditPermission: (docId: number) => {
    return api.get(`/api/permission/check/edit/${docId}`);
  }
};
