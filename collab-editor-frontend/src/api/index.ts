// API统一入口
import { commentApi } from './commentApi';
import { documentApi } from './documentApi';
import { notificationApi } from './notificationApi';
import { permissionApi } from './permissionApi';
import { taskApi } from './taskApi';
import { userApi } from './userApi';

// 导出所有API
export {
  commentApi,
  documentApi,
  notificationApi,
  permissionApi,
  taskApi,
  userApi
};

// 从documentApi中导出versionApi
export { versionApi } from './documentApi';
