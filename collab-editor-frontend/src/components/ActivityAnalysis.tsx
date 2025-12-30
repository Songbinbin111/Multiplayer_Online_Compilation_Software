import React, { useState, useEffect } from 'react';
import { activityApi } from '../api/activityApi';
import type { UserActivity, ActivityTypeStats } from '../api/activityApi';
import {
  Box,
  Container,
  Typography,
  Paper,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  MenuItem,
  Button,
  Pagination,
  CircularProgress,
  Alert,
  LinearProgress,
  Stack,
  Chip,
  Grid
} from '@mui/material';
import {
  BarChart as BarChartIcon,
  Timeline as TimelineIcon,
  FilterList as FilterListIcon,
  Refresh as RefreshIcon,
  EventNote as EventNoteIcon
} from '@mui/icons-material';

const ActivityAnalysis: React.FC = () => {
  const [activities, setActivities] = useState<UserActivity[]>([]);
  const [stats, setStats] = useState<ActivityTypeStats[]>([]);
  const [activeDays, setActiveDays] = useState<number>(0);
  const [recentActivities, setRecentActivities] = useState<UserActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const size = 10;
  const [activityType, setActivityType] = useState('');
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');

  // 获取用户ID
  const getUserId = () => {
    return parseInt(localStorage.getItem('userId') || '0');
  };

  // 获取行为记录列表
  const fetchActivities = async () => {
    try {
      setLoading(true);
      const userId = getUserId();
      const response = await activityApi.getActivityList({
        page,
        size,
        userId,
        activityType,
        startTime,
        endTime
      });

      if (response.code === 200) {
        setActivities(response.data.records);
        setTotal(response.data.total);
      } else {
        setError(response.message || '获取行为记录失败');
      }
    } catch (err: any) {
      setError(err.message || '获取行为记录失败，请检查网络');
    } finally {
      setLoading(false);
    }
  };

  // 获取行为统计
  const fetchStats = async () => {
    try {
      const userId = getUserId();
      const response = await activityApi.getActivityStats({
        userId,
        startTime,
        endTime
      });

      if (response.code === 200) {
        const raw = response.data;
        let list: ActivityTypeStats[] = [];
        if (Array.isArray(raw)) {
          list = raw.map((i: any) => ({
            activityType: String(i?.activityType ?? i?.type ?? '未知'),
            count: Number(i?.count) || 0
          }));
        } else if (Array.isArray(raw?.activityDistribution)) {
          list = raw.activityDistribution.map((i: any) => ({
            activityType: String(i?.activityType ?? i?.type ?? '未知'),
            count: Number(i?.count) || 0
          }));
        } else if (raw && typeof raw === 'object') {
          list = Object.entries(raw)
            .filter(([, v]) => typeof v === 'number' || (v && typeof v === 'object' && 'count' in (v as any)))
            .map(([k, v]: any) => ({
              activityType: String(k),
              count: typeof v === 'number' ? v : Number(v?.count) || 0
            }));
        }
        setStats(Array.isArray(list) ? list : []);
      } else {
        setStats([]);
      }
    } catch (err) {
      console.error('获取行为统计失败:', err);
      setStats([]);
    }
  };

  // 获取活跃天数
  const fetchActiveDays = async () => {
    try {
      const userId = getUserId();
      const response = await activityApi.getActiveDays({
        userId,
        startTime,
        endTime
      });

      if (response.code === 200) {
        const data = response.data;
        const days = typeof data === 'number'
          ? data
          : (typeof data === 'object' && data !== null && 'activeDays' in data
            ? Number((data as any).activeDays)
            : 0);
        setActiveDays(Number.isFinite(days) ? days : 0);
      } else {
        setActiveDays(0);
      }
    } catch (err) {
      console.error('获取活跃天数失败:', err);
      setActiveDays(0);
    }
  };

  // 获取最近行为记录
  const fetchRecentActivities = async () => {
    try {
      const userId = getUserId();
      const response = await activityApi.getRecentActivities({
        userId,
        limit: 10
      });

      if (response.code === 200) {
        setRecentActivities(response.data);
      }
    } catch (err) {
      console.error('获取最近行为记录失败:', err);
    }
  };

  // 初始化数据
  useEffect(() => {
    fetchActivities();
    fetchStats();
    fetchActiveDays();
    fetchRecentActivities();
  }, [page, size, activityType, startTime, endTime]);

  // 处理分页变化
  const handlePageChange = (_event: React.ChangeEvent<unknown>, value: number) => {
    setPage(value);
  };

  // 处理筛选条件变化
  const handleFilterChange = () => {
    setPage(1);
    fetchActivities();
  };

  // 重置筛选条件
  const resetFilters = () => {
    setActivityType('');
    setStartTime('');
    setEndTime('');
    setPage(1);
  };

  // 格式化日期
  const formatDate = (dateString: string | any) => {
    if (!dateString) return '-';
    if (typeof dateString === 'string') {
      const date = new Date(dateString.replace(' ', 'T'));
      return date.toLocaleString();
    }
    return new Date(dateString).toLocaleString();
  };

  return (
    <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom component="h2" sx={{ mb: 4, fontWeight: 'bold' }}>
        用户行为分析
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      {/* 统计概览 */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card elevation={2}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom variant="subtitle2">
                总行为次数
              </Typography>
              <Typography variant="h4" component="div" color="primary.main">
                {(Array.isArray(stats) ? stats.reduce((sum, stat) => sum + (Number(stat.count) || 0), 0) : 0)}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card elevation={2}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom variant="subtitle2">
                活跃天数
              </Typography>
              <Typography variant="h4" component="div" color="success.main">
                {activeDays}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card elevation={2}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom variant="subtitle2">
                行为类型
              </Typography>
              <Typography variant="h4" component="div" color="info.main">
                {stats.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <Card elevation={2}>
            <CardContent>
              <Typography color="textSecondary" gutterBottom variant="subtitle2">
                最近记录
              </Typography>
              <Typography variant="h4" component="div" color="warning.main">
                {recentActivities.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        {/* 行为类型统计图表 */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Paper sx={{ p: 3, height: '100%' }} elevation={2}>
            <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center' }}>
              <BarChartIcon sx={{ mr: 1 }} /> 行为类型分布
            </Typography>

            <Box sx={{ mt: 2 }}>
              {stats.length > 0 ? (
                <Stack spacing={2}>
                  {stats.map((stat, index) => (
                    <Box key={index}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                        <Typography variant="body2">{stat.activityType}</Typography>
                        <Typography variant="body2" fontWeight="bold">{stat.count}</Typography>
                      </Box>
                      <LinearProgress
                        variant="determinate"
                        value={(stat.count / Math.max(...stats.map(s => s.count))) * 100}
                        sx={{ height: 10, borderRadius: 5 }}
                      />
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 4 }}>
                  暂无数据
                </Typography>
              )}
            </Box>
          </Paper>
        </Grid>

        {/* 最近行为记录 */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Paper sx={{ p: 3, height: '100%', maxHeight: 400, overflow: 'auto' }} elevation={2}>
            <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center' }}>
              <TimelineIcon sx={{ mr: 1 }} /> 最近行为记录
            </Typography>

            {recentActivities.length > 0 ? (
              <Stack spacing={2} sx={{ mt: 2 }}>
                {recentActivities.map(activity => (
                  <Box key={activity.id} sx={{ p: 1.5, border: '1px solid #eee', borderRadius: 1 }}>
                    <Typography variant="caption" color="text.secondary" display="block">
                      {formatDate(activity.createdAt)}
                    </Typography>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mt: 0.5 }}>
                      <Chip label={activity.activityType} size="small" color="primary" variant="outlined" />
                    </Box>
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      {activity.description}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            ) : (
              <Typography variant="body2" color="text.secondary" align="center" sx={{ py: 4 }}>
                暂无最近行为记录
              </Typography>
            )}
          </Paper>
        </Grid>

        {/* 行为记录列表 */}
        <Grid size={12}>
          <Paper sx={{ p: 3, mt: 3 }} elevation={2}>
            <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
              <EventNoteIcon sx={{ mr: 1 }} /> 行为记录列表
            </Typography>

            {/* 筛选工具栏 */}
            <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: 'background.default' }}>
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems="center">
                <TextField
                  select
                  label="行为类型"
                  value={activityType}
                  onChange={(e) => setActivityType(e.target.value)}
                  size="small"
                  sx={{ minWidth: 150 }}
                >
                  <MenuItem value="">所有类型</MenuItem>
                  {stats.map((stat, index) => (
                    <MenuItem key={index} value={stat.activityType}>{stat.activityType}</MenuItem>
                  ))}
                </TextField>

                <TextField
                  label="开始时间"
                  type="date"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  size="small"
                />

                <TextField
                  label="结束时间"
                  type="date"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  InputLabelProps={{ shrink: true }}
                  size="small"
                />

                <Box sx={{ flexGrow: 1 }} />

                <Button
                  variant="contained"
                  startIcon={<FilterListIcon />}
                  onClick={handleFilterChange}
                >
                  筛选
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<RefreshIcon />}
                  onClick={resetFilters}
                >
                  重置
                </Button>
              </Stack>
            </Paper>

            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                <CircularProgress />
              </Box>
            ) : activities.length > 0 ? (
              <>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>ID</TableCell>
                        <TableCell>行为类型</TableCell>
                        <TableCell>描述</TableCell>
                        <TableCell>对象类型</TableCell>
                        <TableCell>对象ID</TableCell>
                        <TableCell>时间</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {activities.map(activity => (
                        <TableRow key={activity.id} hover>
                          <TableCell>{activity.id}</TableCell>
                          <TableCell>
                            <Chip label={activity.activityType} size="small" variant="outlined" color="primary" />
                          </TableCell>
                          <TableCell>{activity.description}</TableCell>
                          <TableCell>{activity.objectType || '-'}</TableCell>
                          <TableCell>{activity.objectId || '-'}</TableCell>
                          <TableCell>{formatDate(activity.createdAt)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>

                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
                  <Pagination
                    count={Math.ceil(total / size)}
                    page={page}
                    onChange={handlePageChange}
                    color="primary"
                    showFirstButton
                    showLastButton
                  />
                </Box>
              </>
            ) : (
              <Typography variant="body1" align="center" sx={{ py: 4 }} color="text.secondary">
                暂无行为记录
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Container>
  );
};

export default ActivityAnalysis;
