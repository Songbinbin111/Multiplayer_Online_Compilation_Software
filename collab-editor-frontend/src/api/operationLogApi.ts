import { api } from './request';
import type { AxiosResponse } from 'axios';

export interface OperationLog {
  id: number;
  userId: number;
  username: string;
  operationType: string;
  operationContent: string;
  ipAddress: string;
  userAgent: string;
  success: boolean;
  errorMessage?: string;
  createTime: string;
}

export const operationLogApi = {
  // 获取操作日志列表
  getLogs: (params: { page?: number; size?: number; userId?: number; operationType?: string }) => {
    return api.get('/api/operation-logs', { params }).then((res: AxiosResponse) => res.data);
  },
};
