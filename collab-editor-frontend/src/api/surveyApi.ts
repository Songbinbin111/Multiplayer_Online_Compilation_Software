import { api } from './request';

export interface SurveyStats {
  total: number;
  averageScore: number;
  scoreDistribution: Record<string, number>;
  comments: Array<{
    userId: number;
    username: string;
    score: number;
    comment: string;
    createTime: string;
  }>;
}

export const surveyApi = {
  // 提交问卷
  submit: (data: { userId?: number; score: number; comment: string }) => {
    return api.post('/api/survey/submit', data).then(res => res.data);
  },
  // 获取统计数据
  getStats: () => {
    return api.get('/api/survey/stats').then(res => res.data);
  }
};
