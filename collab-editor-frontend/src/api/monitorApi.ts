import { api } from './request';

export const monitorApi = {
  // 获取系统健康信息
  getSystemHealth() {
    return api.get('/api/monitor/system');
  },
  
  // 获取JVM内存信息
  getJvmMemory() {
    return api.get('/api/monitor/memory');
  },
  
  // 获取CPU使用信息
  getCpuUsage() {
    return api.get('/api/monitor/cpu');
  },
  
  // 获取磁盘使用信息
  getDiskUsage() {
    return api.get('/api/monitor/disk');
  },
  
  // 获取线程使用信息
  getThreadInfo() {
    return api.get('/api/monitor/threads');
  },
  
  // 获取错误日志
  getErrorLogs() {
    return api.get('/api/error-logs');
  },
  
  // 获取调查统计
  getSurveyStats() {
    return api.get('/api/survey/stats');
  },
  
  // 提交调查
  submitSurvey(data: { userId: number; score: number; comment: string }) {
    return api.post('/api/survey/submit', data);
  }
};
