import React, { useEffect, useState } from 'react';
import {
  Box,
  Paper,
  Typography,
  Grid,
  Card,
  CardContent,
  List,
  ListItem,
  ListItemText,
  Divider,
  Rating,
  CircularProgress
} from '@mui/material';
import { surveyApi } from '../../api/surveyApi';
import type { SurveyStats as SurveyStatsType } from '../../api/surveyApi';

const SurveyStats: React.FC = () => {
  const [stats, setStats] = useState<SurveyStatsType | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchStats();
  }, []);

  const fetchStats = async () => {
    setLoading(true);
    try {
      const res = await surveyApi.getStats();
      // res.data should match SurveyStatsType based on SurveyController
      // SurveyController.getStats returns Result<Map<String, Object>>
      // Map contains: total, average, distribution, comments
      setStats(res.data as unknown as SurveyStatsType);
    } catch (error) {
      console.error('获取问卷统计失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}><CircularProgress /></Box>;
  }

  if (!stats) {
    return <Box sx={{ p: 3 }}><Typography>暂无数据</Typography></Box>;
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>用户满意度调查统计</Typography>

      <Grid container spacing={3}>
        {/* 概览 */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>总反馈数</Typography>
              <Typography variant="h3">{stats.total || 0}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>平均评分</Typography>
              <Box sx={{ display: 'flex', alignItems: 'center' }}>
                <Typography variant="h3" sx={{ mr: 2 }}>{stats.averageScore?.toFixed(1) || 0}</Typography>
                <Rating value={stats.averageScore || 0} readOnly precision={0.1} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* 评分分布 */}
        <Grid size={{ xs: 12 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>评分分布</Typography>
              <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                {[5, 4, 3, 2, 1].map(score => (
                  <Paper key={score} sx={{ p: 2, minWidth: 100, textAlign: 'center' }}>
                    <Typography variant="h6">{score} 星</Typography>
                    <Typography variant="h4">{stats.scoreDistribution?.[score.toString()] || 0}</Typography>
                  </Paper>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* 评论列表 */}
        <Grid size={{ xs: 12 }}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>用户反馈详情</Typography>
            <List>
              {stats.comments?.map((comment, index) => (
                <React.Fragment key={index}>
                  {index > 0 && <Divider />}
                  <ListItem alignItems="flex-start">
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Rating value={comment.score} readOnly size="small" />
                          <Typography variant="subtitle2">{comment.username || '匿名用户'}</Typography>
                        </Box>
                      }
                      secondary={
                        <>
                          <Typography component="span" variant="body2" color="textPrimary" sx={{ display: 'block', my: 1 }}>
                            {comment.comment}
                          </Typography>
                          <Typography variant="caption" color="textSecondary">
                            {new Date(comment.createTime).toLocaleString()}
                          </Typography>
                        </>
                      }
                    />
                  </ListItem>
                </React.Fragment>
              ))}
              {(!stats.comments || stats.comments.length === 0) && (
                <ListItem><ListItemText primary="暂无评论" /></ListItem>
              )}
            </List>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default SurveyStats;
