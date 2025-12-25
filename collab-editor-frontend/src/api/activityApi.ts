import { api } from './request';

// 用户活动记录接口定义
export interface UserActivity {
  id: number;
  userId: number;
  activityType: string;
  description: string;
  objectId?: number;
  objectType?: string;
  createdAt: string;
}

// 活动类型统计接口定义
export interface ActivityTypeStats {
  activityType: string;
  count: number;
}

// 用户活动分析相关API
export const activityApi = {
  // 获取用户行为记录列表
  getActivityList: (params: { 
    page?: number; 
    size?: number; 
    userId?: number; 
    activityType?: string;
    startTime?: string;
    endTime?: string;
  }) => {
    return api.get('/api/activity/list', { params }).then(res => res.data);
  },
  
  // 获取用户行为统计
  getActivityStats: (params: { 
    userId?: number; 
    startTime?: string;
    endTime?: string;
  }) => {
    return api.get('/api/activity/stats', { params }).then(res => res.data);
  },
  
  // 获取用户活跃天数
  getActiveDays: (params: { 
    userId: number;
    startTime?: string;
    endTime?: string;
  }) => {
    return api.get('/api/activity/active-days', { params }).then(res => res.data);
  },
  
  // 获取最近行为记录
  getRecentActivities: (params: { 
    userId?: number;
    limit?: number;
  }) => {
    return api.get('/api/activity/recent', { params }).then(res => res.data);
  }
};
