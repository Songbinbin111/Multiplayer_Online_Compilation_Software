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
  CircularProgress
} from '@mui/material';
import { activityApi } from '../../api/activityApi';
import type { UserActivity } from '../../api/activityApi';

const UserBehavior: React.FC = () => {
  const [recentActivities, setRecentActivities] = useState<UserActivity[]>([]);
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const activitiesRes = await activityApi.getRecentActivities({ limit: 20 });
      const activitiesData = activitiesRes?.data ?? activitiesRes ?? [];
      const safeActivities = Array.isArray(activitiesData) ? activitiesData : [];
      setRecentActivities(safeActivities);

      const statsRes = await activityApi.getActivityStats({});
      const raw = statsRes?.data ?? statsRes ?? {};
      const normalized = {
        totalCount: Array.isArray(raw?.activityDistribution)
          ? raw.activityDistribution.reduce((sum: number, i: any) => sum + (Number(i?.count) || 0), 0)
          : 0,
        typeDistribution: Array.isArray(raw?.activityDistribution)
          ? Object.fromEntries(
            raw.activityDistribution.map((i: any) => [String(i?.activityType ?? i?.type ?? '未知'), Number(i?.count) || 0])
          )
          : {}
      };
      setStats(normalized);
    } catch (error) {
      console.error('获取行为数据失败:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}><CircularProgress /></Box>;
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>用户行为分析</Typography>

      <Grid container spacing={3}>
        {/* 统计概览 */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>总活动记录数</Typography>
              <Typography variant="h3">{stats?.totalCount || 0}</Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* 活动类型分布 */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>活动类型分布</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                {stats?.typeDistribution && Object.entries(stats.typeDistribution).map(([type, count]) => (
                  <Paper key={type} sx={{ p: 2, bgcolor: 'background.default' }}>
                    <Typography variant="subtitle2">{type}</Typography>
                    <Typography variant="h6">{count as number}</Typography>
                  </Paper>
                ))}
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* 最近活动列表 */}
        <Grid size={{ xs: 12 }}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>最近用户活动</Typography>
            <List>
              {Array.isArray(recentActivities) && recentActivities.map((activity, index) => (
                <React.Fragment key={activity.id}>
                  {index > 0 && <Divider />}
                  <ListItem>
                    <ListItemText
                      primary={activity.description}
                      secondary={
                        <>
                          <Typography component="span" variant="body2" color="textPrimary">
                            用户ID: {activity.userId}
                          </Typography>
                          {" — " + (() => {
                            const v = activity.createdAt as any;
                            const s = typeof v === 'string' ? v.replace(' ', 'T') : v;
                            const d = new Date(s);
                            return isNaN(d.getTime()) ? String(v) : d.toLocaleString();
                          })()}
                        </>
                      }
                    />
                  </ListItem>
                </React.Fragment>
              ))}
              {recentActivities.length === 0 && (
                <ListItem><ListItemText primary="暂无活动记录" /></ListItem>
              )}
            </List>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};

export default UserBehavior;
