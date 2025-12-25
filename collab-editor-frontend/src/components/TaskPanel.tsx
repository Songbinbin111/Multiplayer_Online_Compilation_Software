import React, { useState, useEffect, useMemo } from 'react';
import { taskApi } from '../api';
import {
  Box,
  Typography,
  Button,
  Card,
  CardContent,
  CardActions,
  TextField,
  Select,
  MenuItem,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Stack,
  IconButton
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';

// 任务接口定义
interface Task {
  id: number;
  docId: number;
  title: string;
  content: string;
  creatorId: number;
  assigneeId: number;
  status: number; // 0-待处理，1-进行中，2-已完成
  createTime: string;
  updateTime: string;
  deadline?: string; // 截止日期
}

// 用户接口定义
interface User {
  id: number;
  username: string;
  nickname?: string;
}

interface TaskPanelProps {
  docId: number;
  onlineUsers: User[];
  currentUserId: number;
}

const TaskPanel: React.FC<TaskPanelProps> = ({ docId, onlineUsers, currentUserId }) => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [newTask, setNewTask] = useState({
    title: '',
    content: '',
    assigneeId: currentUserId,
    deadline: ''
  });

  // 获取文档的所有任务
  const fetchTasks = async () => {
    try {
      const response = await taskApi.getByDocId(docId);
      const data = response.data && response.data.data ? response.data.data : [];
      setTasks(data || []); // 确保tasks始终是数组
    } catch (error) {
      console.error('获取任务列表失败:', error);
      setTasks([]); // 错误时也设置为空数组
    }
  };

  // 监听 currentUserId 变化，更新 newTask.assigneeId
  useEffect(() => {
    if (currentUserId && newTask.assigneeId === 0) {
      setNewTask(prev => ({ ...prev, assigneeId: currentUserId }));
    }
  }, [currentUserId]);

  // 创建任务
  const handleCreateTask = async () => {
    if (!newTask.title.trim()) {
      alert('请输入任务标题');
      return;
    }

    try {
      // 确保有负责人，如果没有则默认为当前用户
      const assigneeId = newTask.assigneeId || currentUserId;

      console.log('正在创建任务:', { ...newTask, assigneeId, docId });

      const response = await taskApi.create({
        docId,
        title: newTask.title,
        content: newTask.content,
        assigneeId: assigneeId,
        deadline: newTask.deadline || undefined
      });

      console.log('创建任务响应:', response);

      if (response.data && response.data.code === 200) {
        setShowCreateForm(false);
        setNewTask({
          title: '',
          content: '',
          assigneeId: currentUserId,
          deadline: ''
        });
        fetchTasks(); // 刷新任务列表
        // alert('创建任务成功');
      } else {
        console.error('创建任务失败:', response.data.message);
        alert('创建任务失败: ' + (response.data.message || '未知错误'));
      }
    } catch (error) {
      console.error('创建任务失败:', error);
      alert('创建任务失败: 网络错误或服务器问题');
    }
  };

  // 更新任务状态
  const handleUpdateStatus = async (taskId: number, newStatus: number) => {
    try {
      // 获取当前任务以获取现有的截止日期
      const task = tasks.find(t => t.id === taskId);
      await taskApi.updateStatus({
        taskId,
        status: newStatus,
        deadline: task?.deadline
      });
      fetchTasks(); // 刷新任务列表
    } catch (error) {
      console.error('更新任务状态失败:', error);
    }
  };

  // 删除任务
  const handleDeleteTask = async (taskId: number) => {
    if (window.confirm('确定要删除这个任务吗？')) {
      try {
        const response = await taskApi.delete(taskId);
        if (response.data && response.data.code === 200) {
          // alert('任务删除成功');
          fetchTasks(); // 刷新任务列表
        } else {
          alert('删除失败: ' + (response.data.message || '未知错误'));
        }
      } catch (error: any) {
        console.error('删除任务失败:', error);
        alert('删除失败: ' + (error.response?.data?.message || '网络错误'));
      }
    }
  };

  // 获取任务状态文本
  const getStatusText = (status: number) => {
    switch (status) {
      case 0: return '待处理';
      case 1: return '进行中';
      case 2: return '已完成';
      default: return '未知';
    }
  };

  // 获取任务状态颜色
  const getStatusColor = (status: number) => {
    switch (status) {
      case 0: return 'warning';
      case 1: return 'info';
      case 2: return 'success';
      default: return 'default';
    }
  };

  // 获取用户名
  const getUsername = (userId: number) => {
    const user = onlineUsers.find(u => u.id === userId);
    return user ? user.username : '未知用户';
  };

  // 组件挂载时获取任务列表
  useEffect(() => {
    fetchTasks();
  }, [docId]);

  // 使用useMemo缓存渲染的任务列表
  const renderedTasks = useMemo(() => {
    if (!Array.isArray(tasks) || tasks.length === 0) {
      return (
        <Box sx={{ p: 2, textAlign: 'center', color: 'text.secondary' }}>
          <Typography variant="body2">暂无任务</Typography>
        </Box>
      );
    }

    return tasks.map(task => {
      // 获取任务负责人用户名
      const assigneeUsername = getUsername(task.assigneeId);
      const isCreatorOrAssignee = task.assigneeId === currentUserId || task.creatorId === currentUserId;

      return (
        <Card key={task.id} sx={{ mb: 2, variant: 'outlined' }}>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="subtitle1" component="div" sx={{ fontWeight: 'bold' }}>
                {task.title}
              </Typography>
              <Chip
                label={getStatusText(task.status)}
                color={getStatusColor(task.status) as any}
                size="small"
              />
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
              {task.content}
            </Typography>
            <Stack direction="row" spacing={1} sx={{ mb: 1, fontSize: '0.8rem', color: 'text.secondary' }}>
              <Typography variant="caption">创建者: {getUsername(task.creatorId)}</Typography>
              <Typography variant="caption">负责人: {assigneeUsername}</Typography>
            </Stack>
            {task.deadline && (
              <Typography variant="caption" display="block" color="error">
                截止日期: {new Date(task.deadline).toLocaleString()}
              </Typography>
            )}
          </CardContent>
          <CardActions sx={{ justifyContent: 'space-between' }}>
            {isCreatorOrAssignee ? (
              <Box>
                <Button size="small" onClick={() => handleUpdateStatus(task.id, 0)} disabled={task.status === 0}>待处理</Button>
                <Button size="small" onClick={() => handleUpdateStatus(task.id, 1)} disabled={task.status === 1}>进行中</Button>
                <Button size="small" onClick={() => handleUpdateStatus(task.id, 2)} disabled={task.status === 2}>已完成</Button>
              </Box>
            ) : <Box />}

            {task.creatorId === currentUserId && (
              <IconButton size="small" color="error" onClick={() => handleDeleteTask(task.id)}>
                <DeleteIcon />
              </IconButton>
            )}
          </CardActions>
        </Card>
      );
    });
  }, [tasks, onlineUsers, currentUserId]);

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6">任务列表</Typography>
        <Button
          startIcon={<AddIcon />}
          variant="contained"
          size="small"
          onClick={() => setShowCreateForm(true)}
        >
          创建
        </Button>
      </Box>

      <Box sx={{ flexGrow: 1, overflow: 'auto', p: 2 }}>
        {renderedTasks}
      </Box>

      {/* 创建任务对话框 */}
      <Dialog
        open={showCreateForm}
        onClose={() => setShowCreateForm(false)}
        fullWidth
        maxWidth="sm"
        sx={{ zIndex: 1400 }} // 确保在最上层
      >
        <DialogTitle>创建新任务</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              label="任务标题"
              fullWidth
              value={newTask.title}
              onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
            />
            <TextField
              label="任务内容"
              fullWidth
              multiline
              rows={3}
              value={newTask.content}
              onChange={(e) => setNewTask({ ...newTask, content: e.target.value })}
            />
            <FormControl fullWidth>
              <InputLabel>负责人</InputLabel>
              <Select
                value={newTask.assigneeId}
                label="负责人"
                onChange={(e) => setNewTask({ ...newTask, assigneeId: Number(e.target.value) })}
              >
                {onlineUsers.map(user => (
                  <MenuItem key={user.id} value={user.id}>
                    {user.username}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="截止日期"
              type="datetime-local"
              fullWidth
              InputLabelProps={{ shrink: true }}
              value={newTask.deadline ? new Date(newTask.deadline).toISOString().slice(0, 16) : ''}
              onChange={(e) => setNewTask({ ...newTask, deadline: e.target.value })}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowCreateForm(false)}>取消</Button>
          <Button onClick={handleCreateTask} variant="contained">保存</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default TaskPanel;
