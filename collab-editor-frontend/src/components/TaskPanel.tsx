import React, { useState, useEffect } from 'react';
import { taskApi } from '../api';

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
    assigneeId: currentUserId
  });

  // 获取文档的所有任务
  const fetchTasks = async () => {
    try {
      const response = await taskApi.getByDocId(docId);
      const data = response.data;
      setTasks(data);
    } catch (error) {
      console.error('获取任务列表失败:', error);
    }
  };

  // 创建任务
  const handleCreateTask = async () => {
    if (!newTask.title.trim()) return;

    try {
      await taskApi.create({
        docId,
        title: newTask.title,
        content: newTask.content,
        assigneeId: newTask.assigneeId
      });

      setShowCreateForm(false);
      setNewTask({
        title: '',
        content: '',
        assigneeId: currentUserId
      });
      fetchTasks(); // 刷新任务列表
    } catch (error) {
      console.error('创建任务失败:', error);
    }
  };

  // 更新任务状态
  const handleUpdateStatus = async (taskId: number, newStatus: number) => {
    try {
      await taskApi.updateStatus({ taskId, status: newStatus });
      fetchTasks(); // 刷新任务列表
    } catch (error) {
      console.error('更新任务状态失败:', error);
    }
  };

  // 删除任务
  const handleDeleteTask = async (taskId: number) => {
    if (window.confirm('确定要删除这个任务吗？')) {
      try {
        await taskApi.delete(taskId);
        fetchTasks(); // 刷新任务列表
      } catch (error) {
        console.error('删除任务失败:', error);
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

  // 获取任务状态样式
  const getStatusClass = (status: number) => {
    switch (status) {
      case 0: return 'status-pending';
      case 1: return 'status-in-progress';
      case 2: return 'status-completed';
      default: return '';
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

  return (
    <div className="task-panel">
      <div className="task-panel-header">
        <h3>任务列表</h3>
        <button
          className="create-task-btn"
          onClick={() => setShowCreateForm(!showCreateForm)}
        >
          {showCreateForm ? '取消' : '创建任务'}
        </button>
      </div>

      {/* 创建任务表单 */}
      {showCreateForm && (
        <div className="create-task-form">
          <input
            type="text"
            placeholder="任务标题"
            value={newTask.title}
            onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
            className="task-title-input"
          />
          <textarea
            placeholder="任务内容"
            value={newTask.content}
            onChange={(e) => setNewTask({ ...newTask, content: e.target.value })}
            className="task-content-textarea"
          />
          <select
            value={newTask.assigneeId}
            onChange={(e) => setNewTask({ ...newTask, assigneeId: parseInt(e.target.value) })}
            className="task-assignee-select"
          >
            {onlineUsers.map(user => (
              <option key={user.id} value={user.id}>
                {user.username}
              </option>
            ))}
          </select>
          <div className="task-form-actions">
            <button onClick={handleCreateTask} className="save-task-btn">
              保存
            </button>
          </div>
        </div>
      )}

      {/* 任务列表 */}
      <div className="task-list">
        {tasks.length === 0 ? (
          <p className="no-tasks">暂无任务</p>
        ) : (
          tasks.map(task => (
            <div key={task.id} className="task-item">
              <div className="task-header">
                <h4 className="task-title">{task.title}</h4>
                <span className={`task-status ${getStatusClass(task.status)}`}>
                  {getStatusText(task.status)}
                </span>
              </div>
              <div className="task-content">{task.content}</div>
              <div className="task-meta">
                <span>创建者: {getUsername(task.creatorId)}</span>
                <span>负责人: {getUsername(task.assigneeId)}</span>
              </div>
              <div className="task-actions">
                {/* 状态更新按钮 */}
                {(task.assigneeId === currentUserId || task.creatorId === currentUserId) && (
                  <div className="task-status-buttons">
                    <button
                      onClick={() => handleUpdateStatus(task.id, 0)}
                      className={`status-btn ${task.status === 0 ? 'active' : ''}`}
                    >
                      待处理
                    </button>
                    <button
                      onClick={() => handleUpdateStatus(task.id, 1)}
                      className={`status-btn ${task.status === 1 ? 'active' : ''}`}
                    >
                      进行中
                    </button>
                    <button
                      onClick={() => handleUpdateStatus(task.id, 2)}
                      className={`status-btn ${task.status === 2 ? 'active' : ''}`}
                    >
                      已完成
                    </button>
                  </div>
                )}
                {/* 删除按钮 */}
                {task.creatorId === currentUserId && (
                  <button
                    onClick={() => handleDeleteTask(task.id)}
                    className="delete-task-btn"
                  >
                    删除
                  </button>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default TaskPanel;
