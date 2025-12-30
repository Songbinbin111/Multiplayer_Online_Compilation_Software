import React, { useEffect, useState } from 'react';
import { Card, CardContent, Typography, Box, Paper, LinearProgress, Button, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tabs, Tab, Dialog, DialogTitle, DialogContent, DialogActions, FormControl, InputLabel, Select, MenuItem, Pagination, TextField, Rating } from '@mui/material';
import type { SelectChangeEvent } from '@mui/material/Select';
import RefreshIcon from '@mui/icons-material/Refresh';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import WarningIcon from '@mui/icons-material/Warning';
import InfoIcon from '@mui/icons-material/Info';
import EditIcon from '@mui/icons-material/Edit';
import { errorLogger } from '../utils/errorLogger';
import { operationLogApi, type OperationLog } from '../api/operationLogApi';
import api from '../api/request';

// 用户数据类型
interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  createTime: string;
}

// 用户行为统计类型
interface UserStats {
  activeUsers: { username: string; count: number }[];
  operationDistribution: { operation: string; count: number }[];
}

// 调查问卷统计类型
interface SurveyStats {
  total: number;
  averageScore: string;
  recentComments: {
    id: number;
    score: number;
    comment: string;
    createTime: string;
  }[];
}

// 系统健康数据类型
interface SystemHealth {
  applicationName: string;
  activeProfile: string;
  javaVersion: string;
  javaHome: string;
  osName: string;
  osVersion: string;
  osArch: string;
  startTime: string;
  uptime: string;
}

// JVM内存使用数据类型
interface JvmMemory {
  totalMemory: number;
  maxMemory: number;
  freeMemory: number;
  usedMemory: number;
  usedMemoryPercentage: number;
}

// CPU使用数据类型
interface CpuUsage {
  availableProcessors: number;
  systemCpuLoad: number;
  processCpuLoad: number;
}

// 磁盘使用数据类型
interface DiskInfo {
  path: string;
  totalSpace: number;
  freeSpace: number;
  usableSpace: number;
  usedSpace: number;
  usedPercentage: number;
}

interface DiskUsage {
  disks: DiskInfo[];
}

// 错误日志数据类型
interface ErrorLog {
  id: number;
  timestamp: string;
  type: string;
  message: string;
  url?: string;
  line?: number;
  column?: number;
  userAgent: string;
  userId?: number;
  docId?: number;
  createTime: string;
}

// 线程使用数据类型
interface ThreadInfo {
  threadCount: number;
  peakThreadCount: number;
  daemonThreadCount: number;
  totalStartedThreadCount: number;
  deadlockedThreadCount: number;
}

const MonitorDashboard: React.FC = () => {
  // 状态管理
  const [systemHealth, setSystemHealth] = useState<SystemHealth | null>(null);
  const [jvmMemory, setJvmMemory] = useState<JvmMemory | null>(null);
  const [cpuUsage, setCpuUsage] = useState<CpuUsage | null>(null);
  const [diskUsage, setDiskUsage] = useState<DiskUsage | null>(null);
  const [errorLogs, setErrorLogs] = useState<ErrorLog[]>([]);
  const [operationLogs, setOperationLogs] = useState<OperationLog[]>([]);
  const [threadInfo, setThreadInfo] = useState<ThreadInfo | null>(null);
  // 用户管理状态
  const [users, setUsers] = useState<User[]>([]);
  const [userTotal, setUserTotal] = useState(0);
  const [userPage, setUserPage] = useState(1);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [newRole, setNewRole] = useState('');
  const [showRoleDialog, setShowRoleDialog] = useState(false);
  // 用户行为分析状态
  const [userStats, setUserStats] = useState<UserStats>({ activeUsers: [], operationDistribution: [] });
  // 满意度调查状态
  const [surveyStats, setSurveyStats] = useState<SurveyStats | null>(null);
  const [showSurveyDialog, setShowSurveyDialog] = useState(false);
  const [surveyScore, setSurveyScore] = useState(5);
  const [surveyComment, setSurveyComment] = useState('');

  const [isLoading, setIsLoading] = useState(false);
  const [activeTab, setActiveTab] = useState(0);

  // 获取系统健康信息
  const fetchSystemHealth = async () => {
    try {
      const response = await fetch('/api/monitor/system');
      if (response.ok) {
        const data = await response.json();
        setSystemHealth(data.data);
      }
    } catch (error) {
      console.error('Failed to fetch system health:', error);
      errorLogger.logError(error as Error, 'APIError');
    }
  };

  // 获取JVM内存信息
  const fetchJvmMemory = async () => {
    try {
      const response = await fetch('/api/monitor/memory');
      if (response.ok) {
        const data = await response.json();
        setJvmMemory(data.data);
      }
    } catch (error) {
      console.error('Failed to fetch JVM memory:', error);
      errorLogger.logError(error as Error, 'APIError');
    }
  };

  // 获取CPU使用信息
  const fetchCpuUsage = async () => {
    try {
      const response = await fetch('/api/monitor/cpu');
      if (response.ok) {
        const data = await response.json();
        setCpuUsage(data.data);
      }
    } catch (error) {
      console.error('Failed to fetch CPU usage:', error);
      errorLogger.logError(error as Error, 'APIError');
    }
  };

  // 获取磁盘使用信息
  const fetchDiskUsage = async () => {
    try {
      const response = await fetch('/api/monitor/disk');
      if (response.ok) {
        const data = await response.json();
        setDiskUsage(data.data);
      }
    } catch (error) {
      console.error('Failed to fetch disk usage:', error);
      errorLogger.logError(error as Error, 'APIError');
    }
  };

  // 获取错误日志信息
  const fetchErrorLogs = async () => {
    try {
      const response = await fetch('/api/error-logs');
      if (response.ok) {
        const data = await response.json();
        setErrorLogs(data.data || []);
      }
    } catch (error) {
      console.error('Failed to fetch error logs:', error);
      errorLogger.logError(error as Error, 'APIError');
      setErrorLogs([]);
    }
  };

  // 获取操作日志信息
  const fetchOperationLogs = async () => {
    try {
      const res = await operationLogApi.getLogs({ size: 20 });
      if (res && res.data && res.data.logs) {
        setOperationLogs(res.data.logs);
      } else if (res && res.records) {
        setOperationLogs(res.records);
      } else {
        setOperationLogs([]);
      }
    } catch (error) {
      console.error('Failed to fetch operation logs:', error);
      setOperationLogs([]);
    }
  };

  // 获取线程使用信息
  const fetchThreadInfo = async () => {
    try {
      const response = await fetch('/api/monitor/threads');
      if (response.ok) {
        const data = await response.json();
        setThreadInfo(data.data);
      }
    } catch (error) {
      console.error('Failed to fetch thread info:', error);
      errorLogger.logError(error as Error, 'APIError');
    }
  };

  // 获取用户列表
  const fetchUsers = async (page: number = 1) => {
    try {
      const response = await api.get(`/api/user/list/page?page=${page}&pageSize=10`);
      if (response.data.code === 200) {
        setUsers(response.data.data.users);
        setUserTotal(response.data.data.total);
        setUserPage(response.data.data.current);
      }
    } catch (error) {
      console.error('Failed to fetch users:', error);
      errorLogger.logError(error as Error, 'APIError');
    }
  };

  // 计算用户行为统计
  const calculateUserBehavior = () => {
    if (!operationLogs.length) return;

    // 统计活跃用户
    const userCount: Record<string, number> = {};
    operationLogs.forEach(log => {
      const username = log.username || 'Unknown';
      userCount[username] = (userCount[username] || 0) + 1;
    });

    const activeUsers = Object.entries(userCount)
      .map(([username, count]) => ({ username, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 10); // Top 10

    // 统计操作类型分布
    const opCount: Record<string, number> = {};
    operationLogs.forEach(log => {
      const op = log.operationType || 'Unknown';
      opCount[op] = (opCount[op] || 0) + 1;
    });

    const operationDistribution = Object.entries(opCount)
      .map(([operation, count]) => ({ operation, count }))
      .sort((a, b) => b.count - a.count);

    setUserStats({ activeUsers, operationDistribution });
  };

  useEffect(() => {
    calculateUserBehavior();
  }, [operationLogs]);

  // 更新用户角色
  const handleUpdateRole = async () => {
    if (!editingUser || !newRole) return;

    try {
      const response = await api.post(`/api/user/update-role?userId=${editingUser.id}&newRole=${newRole}`);
      if (response.data.code === 200) {
        // 刷新列表
        fetchUsers(userPage);
        setShowRoleDialog(false);
        setEditingUser(null);
        setNewRole('');
      } else {
        alert('更新失败: ' + response.data.message);
      }
    } catch (error) {
      console.error('Failed to update role:', error);
      alert('更新失败，请检查网络');
    }
  };

  const openRoleDialog = (user: User) => {
    setEditingUser(user);
    setNewRole(user.role);
    setShowRoleDialog(true);
  };

  // 获取调查统计
  const fetchSurveyStats = async () => {
    try {
      const response = await api.get('/api/survey/stats');
      if (response.data.code === 200) {
        setSurveyStats(response.data.data);
      }
    } catch (error) {
      console.error('Failed to fetch survey stats:', error);
    }
  };

  // 提交调查（模拟）
  const handleSubmitSurvey = async () => {
    try {
      const userIdStr = localStorage.getItem('userId');
      const userId = userIdStr ? parseInt(userIdStr) : 0;

      if (!userId) {
        alert('请先登录');
        return;
      }

      const response = await api.post('/api/survey/submit', {
        userId: userId,
        score: surveyScore,
        comment: surveyComment
      });

      if (response.data.code === 200) {
        alert('感谢您的反馈！');
        setShowSurveyDialog(false);
        setSurveyComment('');
        fetchSurveyStats();
      } else {
        alert('提交失败: ' + response.data.message);
      }
    } catch (error) {
      console.error('Failed to submit survey:', error);
    }
  };

  // 获取所有监控数据
  const fetchAllMonitoringData = async () => {
    setIsLoading(true);
    await Promise.all([
      fetchSystemHealth(),
      fetchJvmMemory(),
      fetchCpuUsage(),
      fetchDiskUsage(),
      fetchErrorLogs(),
      fetchOperationLogs(),
      fetchThreadInfo(),
      fetchUsers(userPage),
      fetchSurveyStats()
    ]);
    setIsLoading(false);
  };

  // 初始化和定期刷新数据
  useEffect(() => {
    fetchAllMonitoringData();
    const interval = setInterval(fetchAllMonitoringData, 30000); // 每30秒刷新一次
    return () => clearInterval(interval);
  }, []);

  // 获取错误类型的图标
  const getErrorTypeIcon = (type: string) => {
    switch (type) {
      case 'GlobalError':
        return <ErrorOutlineIcon color="error" />;
      case 'UnhandledRejection':
        return <WarningIcon color="warning" />;
      default:
        return <InfoIcon color="info" />;
    }
  };

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        系统监控仪表板
      </Typography>

      <Tabs value={activeTab} onChange={handleTabChange} sx={{ mb: 3 }}>
        <Tab label="系统概览" />
        <Tab label="操作日志" />
        <Tab label="错误日志" />
        <Tab label="用户管理" />
        <Tab label="用户行为分析" />
        <Tab label="用户满意度" />
      </Tabs>

      {/* 刷新按钮 */}
      <Box sx={{ mb: 3 }}>
        <Button
          variant="contained"
          startIcon={<RefreshIcon />}
          onClick={fetchAllMonitoringData}
          disabled={isLoading}
        >
          {isLoading ? '刷新中...' : '手动刷新'}
        </Button>
      </Box>

      {/* 系统概览 */}
      {activeTab === 0 && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3, mb: 3 }}>
          {/* 系统健康状态卡片 */}
          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 50%', lg: '1 1 33.33%' } }}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  系统健康状态
                </Typography>
                {systemHealth ? (
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      应用名称: {systemHealth.applicationName}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      环境: {systemHealth.activeProfile}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      运行时间: {systemHealth.uptime}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      Java版本: {systemHealth.javaVersion}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Java安装路径: {systemHealth.javaHome}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      操作系统: {systemHealth.osName} {systemHealth.osVersion} ({systemHealth.osArch})
                    </Typography>
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    加载中...
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Box>

          {/* JVM内存使用卡片 */}
          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 50%', lg: '1 1 33.33%' } }}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  JVM内存使用
                </Typography>
                {jvmMemory ? (
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      总内存: {jvmMemory.totalMemory} MB
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      最大内存: {jvmMemory.maxMemory} MB
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      已用内存: {jvmMemory.usedMemory} MB
                    </Typography>
                    <LinearProgress
                      variant="determinate"
                      value={jvmMemory.usedMemoryPercentage}
                      sx={{
                        my: 1,
                        height: 8,
                        borderRadius: 5,
                        backgroundColor: '#e0e0e0',
                        '& .MuiLinearProgress-bar': {
                          backgroundColor: jvmMemory.usedMemoryPercentage > 80 ? '#f44336' : '#4caf50'
                        }
                      }}
                    />
                    <Typography variant="body2" color="text.secondary" align="right">
                      使用率: {jvmMemory.usedMemoryPercentage}%
                    </Typography>
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    加载中...
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Box>

          {/* CPU使用卡片 */}
          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 50%', lg: '1 1 33.33%' } }}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  CPU使用情况
                </Typography>
                {cpuUsage ? (
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      可用处理器: {cpuUsage.availableProcessors}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      系统负载: {cpuUsage.systemCpuLoad}%
                    </Typography>
                    <LinearProgress
                      variant="determinate"
                      value={cpuUsage.systemCpuLoad}
                      sx={{
                        my: 1,
                        height: 8,
                        borderRadius: 5,
                        backgroundColor: '#e0e0e0',
                        '& .MuiLinearProgress-bar': {
                          backgroundColor: cpuUsage.systemCpuLoad > 80 ? '#f44336' : '#4caf50'
                        }
                      }}
                    />
                    <Typography variant="body2" color="text.secondary">
                      进程CPU负载: {cpuUsage.processCpuLoad}%
                    </Typography>
                    <LinearProgress
                      variant="determinate"
                      value={cpuUsage.processCpuLoad}
                      sx={{
                        my: 1,
                        height: 8,
                        borderRadius: 5,
                        backgroundColor: '#e0e0e0',
                        '& .MuiLinearProgress-bar': {
                          backgroundColor: cpuUsage.processCpuLoad > 80 ? '#f44336' : '#4caf50'
                        }
                      }}
                    />
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    加载中...
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Box>

          {/* 磁盘使用卡片 */}
          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 100%', lg: '1 1 100%' } }}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  磁盘使用情况
                </Typography>
                {diskUsage && diskUsage.disks ? (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
                    {diskUsage.disks.map((disk, index) => (
                      <Box key={index} sx={{ flex: '1 1 300px', p: 1, border: '1px solid #eee', borderRadius: 1 }}>
                        <Typography variant="subtitle2" gutterBottom>
                          {disk.path}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          总量: {disk.totalSpace} GB | 可用: {disk.freeSpace} GB
                        </Typography>
                        <LinearProgress
                          variant="determinate"
                          value={disk.usedPercentage}
                          sx={{
                            my: 1,
                            height: 8,
                            borderRadius: 5,
                            backgroundColor: '#e0e0e0',
                            '& .MuiLinearProgress-bar': {
                              backgroundColor: disk.usedPercentage > 80 ? '#f44336' : '#4caf50'
                            }
                          }}
                        />
                        <Typography variant="body2" color="text.secondary" align="right">
                          已用: {disk.usedPercentage}%
                        </Typography>
                      </Box>
                    ))}
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    加载中...
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Box>

          {/* 线程信息卡片 */}
          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 50%', lg: '1 1 33.33%' } }}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  线程信息
                </Typography>
                {threadInfo ? (
                  <Box>
                    <Typography variant="body2" color="text.secondary">
                      当前线程数: {threadInfo.threadCount}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      峰值线程数: {threadInfo.peakThreadCount}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      守护线程数: {threadInfo.daemonThreadCount}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      启动线程总数: {threadInfo.totalStartedThreadCount}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ color: threadInfo.deadlockedThreadCount > 0 ? 'error.main' : 'text.secondary' }}>
                      死锁线程数: {threadInfo.deadlockedThreadCount}
                    </Typography>
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    加载中...
                  </Typography>
                )}
              </CardContent>
            </Card>
          </Box>
        </Box>
      )}

      {/* 操作日志 */}
      {activeTab === 1 && (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>操作人</TableCell>
                <TableCell>操作类型</TableCell>
                <TableCell>详情</TableCell>
                <TableCell>IP地址</TableCell>
                <TableCell>时间</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {operationLogs.map((log) => (
                <TableRow key={log.id}>
                  <TableCell>{log.id}</TableCell>
                  <TableCell>{log.username} (ID: {log.userId})</TableCell>
                  <TableCell>{log.operationType}</TableCell>
                  <TableCell>{log.operationContent}</TableCell>
                  <TableCell>{log.ipAddress}</TableCell>
                  <TableCell>{log.createTime}</TableCell>
                </TableRow>
              ))}
              {operationLogs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">暂无操作日志</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* 错误日志 */}
      {activeTab === 2 && (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>类型</TableCell>
                <TableCell>消息</TableCell>
                <TableCell>URL</TableCell>
                <TableCell>用户ID</TableCell>
                <TableCell>时间</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {errorLogs.map((log) => (
                <TableRow key={log.id}>
                  <TableCell>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {getErrorTypeIcon(log.type)}
                      {log.type}
                    </Box>
                  </TableCell>
                  <TableCell>{log.message}</TableCell>
                  <TableCell>{log.url}</TableCell>
                  <TableCell>{log.userId || '-'}</TableCell>
                  <TableCell>{new Date(log.createTime).toLocaleString()}</TableCell>
                </TableRow>
              ))}
              {errorLogs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center">暂无错误日志</TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* 用户管理 */}
      {activeTab === 3 && (
        <Box>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>用户名</TableCell>
                  <TableCell>邮箱</TableCell>
                  <TableCell>角色</TableCell>
                  <TableCell>注册时间</TableCell>
                  <TableCell>操作</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell>{user.id}</TableCell>
                    <TableCell>{user.username}</TableCell>
                    <TableCell>{user.email || '-'}</TableCell>
                    <TableCell>
                      <Box sx={{
                        color: user.role === 'admin' ? 'error.main' : 'text.primary',
                        fontWeight: user.role === 'admin' ? 'bold' : 'normal'
                      }}>
                        {user.role}
                      </Box>
                    </TableCell>
                    <TableCell>{user.createTime ? new Date(user.createTime).toLocaleString() : '-'}</TableCell>
                    <TableCell>
                      <Button
                        startIcon={<EditIcon />}
                        size="small"
                        onClick={() => openRoleDialog(user)}
                      >
                        修改角色
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
                {users.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center">暂无用户数据</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
          <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
            <Pagination
              count={Math.ceil(userTotal / 10)}
              page={userPage}
              onChange={(_, page) => {
                setUserPage(page);
                fetchUsers(page);
              }}
              color="primary"
            />
          </Box>
        </Box>
      )}

      {/* 用户行为分析 */}
      {activeTab === 4 && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
          {/* 活跃用户排名 */}
          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 48%' } }}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  最活跃用户 (Top 10)
                </Typography>
                {userStats.activeUsers.length > 0 ? (
                  <TableContainer>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>排名</TableCell>
                          <TableCell>用户名</TableCell>
                          <TableCell align="right">操作次数</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {userStats.activeUsers.map((user, index) => (
                          <TableRow key={user.username}>
                            <TableCell>{index + 1}</TableCell>
                            <TableCell>{user.username}</TableCell>
                            <TableCell align="right">{user.count}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                ) : (
                  <Typography variant="body2" color="text.secondary">暂无数据</Typography>
                )}
              </CardContent>
            </Card>
          </Box>

          {/* 操作类型分布 */}
          <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 48%' } }}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom color="primary">
                  操作类型分布
                </Typography>
                {userStats.operationDistribution.length > 0 ? (
                  <Box>
                    {userStats.operationDistribution.map((op) => (
                      <Box key={op.operation} sx={{ mb: 2 }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                          <Typography variant="body2">{op.operation}</Typography>
                          <Typography variant="body2">{op.count}次</Typography>
                        </Box>
                        <LinearProgress
                          variant="determinate"
                          value={(op.count / userStats.operationDistribution[0].count) * 100}
                          sx={{ height: 8, borderRadius: 4 }}
                        />
                      </Box>
                    ))}
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">暂无数据</Typography>
                )}
              </CardContent>
            </Card>
          </Box>
        </Box>
      )}

      {/* 用户满意度 */}
      {activeTab === 5 && (
        <Box>
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'flex-end' }}>
            <Button variant="outlined" onClick={() => setShowSurveyDialog(true)}>
              模拟用户提交反馈
            </Button>
          </Box>

          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 3 }}>
            {/* 总体评分 */}
            <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 30%' } }}>
              <Card sx={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <CardContent sx={{ textAlign: 'center' }}>
                  <Typography variant="h6" gutterBottom color="text.secondary">
                    平均满意度评分
                  </Typography>
                  <Typography variant="h2" color="primary">
                    {(Number(surveyStats?.averageScore ?? 0)).toFixed(1)}
                  </Typography>
                  <Rating value={Number(surveyStats?.averageScore ?? 0)} readOnly precision={0.1} size="large" />
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    共收到 {surveyStats?.total || 0} 条反馈
                  </Typography>
                </CardContent>
              </Card>
            </Box>

            {/* 最新反馈 */}
            <Box sx={{ flex: { xs: '1 1 100%', md: '1 1 65%' } }}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom color="primary">
                    最新用户反馈
                  </Typography>
                  {surveyStats && surveyStats.recentComments && surveyStats.recentComments.length > 0 ? (
                    <Box>
                      {surveyStats.recentComments.map((item) => (
                        <Box key={item.id} sx={{ mb: 2, pb: 2, borderBottom: '1px solid #eee' }}>
                          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                            <Rating value={item.score} readOnly size="small" />
                            <Typography variant="caption" color="text.secondary">
                              {new Date(item.createTime).toLocaleString()}
                            </Typography>
                          </Box>
                          <Typography variant="body2">
                            {item.comment || '无评论内容'}
                          </Typography>
                        </Box>
                      ))}
                    </Box>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      暂无反馈数据
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Box>
          </Box>
        </Box>
      )}

      {/* 修改角色对话框 */}
      <Dialog open={showRoleDialog} onClose={() => setShowRoleDialog(false)}>
        <DialogTitle>修改用户角色</DialogTitle>
        <DialogContent sx={{ minWidth: 300, pt: 2 }}>
          <Box sx={{ mt: 1 }}>
            <Typography variant="subtitle1" gutterBottom>
              用户: {editingUser?.username}
            </Typography>
            <FormControl fullWidth margin="normal">
              <InputLabel>角色</InputLabel>
              <Select
                value={newRole}
                label="角色"
                onChange={(e: SelectChangeEvent) => setNewRole(e.target.value)}
              >
                <MenuItem value="user">普通用户 (user)</MenuItem>
                <MenuItem value="admin">管理员 (admin)</MenuItem>
                <MenuItem value="viewer">访客 (viewer)</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowRoleDialog(false)}>取消</Button>
          <Button onClick={handleUpdateRole} variant="contained" color="primary">
            保存
          </Button>
        </DialogActions>
      </Dialog>

      {/* 提交反馈对话框 */}
      <Dialog open={showSurveyDialog} onClose={() => setShowSurveyDialog(false)}>
        <DialogTitle>提交满意度反馈</DialogTitle>
        <DialogContent sx={{ minWidth: 300, pt: 2 }}>
          <Box sx={{ mt: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box>
              <Typography component="legend">评分</Typography>
              <Rating
                name="survey-rating"
                value={surveyScore}
                onChange={(_, newValue) => {
                  setSurveyScore(newValue || 5);
                }}
              />
            </Box>
            <TextField
              label="您的建议或反馈"
              multiline
              rows={4}
              value={surveyComment}
              onChange={(e) => setSurveyComment(e.target.value)}
              fullWidth
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowSurveyDialog(false)}>取消</Button>
          <Button onClick={handleSubmitSurvey} variant="contained" color="primary">
            提交
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default MonitorDashboard;
