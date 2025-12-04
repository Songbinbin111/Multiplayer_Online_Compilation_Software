import React, { useState, useEffect } from 'react';
import { permissionApi, userApi } from '../api';

// 定义权限类型
interface DocPermission {
  id: number;
  docId: number;
  userId: number;
  username?: string;
  permissionType: number; // 0-查看，1-编辑
  createTime: string;
}

// 定义用户类型
interface User {
  id: number;
  username: string;
}

// 定义组件属性
interface PermissionPanelProps {
  docId: number;
  currentUserId: number;
  onlineUsers: Array<{ userId: number; username: string }>;
}

const PermissionPanel: React.FC<PermissionPanelProps> = ({ docId, currentUserId, onlineUsers }) => {
  const [permissions, setPermissions] = useState<DocPermission[]>([]);
  const [allUsers, setAllUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<number | null>(null);
  const [selectedPermission, setSelectedPermission] = useState<number>(0);
  const [isOwner, setIsOwner] = useState(false);

  // 获取文档权限列表
  const fetchPermissions = async () => {
    try {
      const response = await permissionApi.getPermissionsByDocId(docId);
      const data = response.data;
      setPermissions(data);
      // 检查当前用户是否是文档所有者
      const userPermission = data.find((p: DocPermission) => p.userId === currentUserId);
      if (userPermission && userPermission.permissionType === 1) {
        setIsOwner(true);
      }
    } catch (error) {
      console.error('获取权限列表失败:', error);
    }
  };

  // 获取所有用户列表
  const fetchAllUsers = async () => {
    try {
      const response = await userApi.getList();
      const data = response.data;
      setAllUsers(data);
    } catch (error) {
      console.error('获取用户列表失败:', error);
    }
  };

  // 分配权限
  const handleAssignPermission = async () => {
    if (!selectedUser) return;

    try {
      await permissionApi.assignPermission(docId, selectedUser, selectedPermission);
      fetchPermissions();
      setSelectedUser(null);
      setSelectedPermission(0);
    } catch (error) {
      console.error('分配权限失败:', error);
    }
  };

  // 更新权限
  const handleUpdatePermission = async (userId: number, permissionType: number) => {
    try {
      await permissionApi.updatePermission(docId, userId, permissionType);
      fetchPermissions();
    } catch (error) {
      console.error('更新权限失败:', error);
    }
  };

  // 移除权限
  const handleRemovePermission = async (userId: number) => {
    if (window.confirm('确定要移除该用户的权限吗？')) {
      try {
        await permissionApi.removePermission(docId, userId);
        fetchPermissions();
      } catch (error) {
        console.error('移除权限失败:', error);
      }
    }
  };

  // 获取用户名
  const getUsername = (userId: number) => {
    const user = onlineUsers.find(u => u.userId === userId);
    if (user) return user.username;

    const allUser = allUsers.find(u => u.id === userId);
    if (allUser) return allUser.username;

    return '未知用户';
  };

  // 获取权限类型文本
  const getPermissionText = (type: number) => {
    return type === 0 ? '查看' : '编辑';
  };

  // 检查用户是否已有权限
  const hasPermission = (userId: number) => {
    return permissions.some(p => p.userId === userId);
  };

  // 组件挂载时获取权限列表和用户列表
  useEffect(() => {
    fetchPermissions();
    fetchAllUsers();
  }, [docId]);

  return (
    <div className="permission-panel">
      <div className="permission-panel-header">
        <h3>文档权限</h3>
      </div>

      {/* 分配权限表单 */}
      {isOwner && (
        <div className="assign-permission-form">
          <h4>分配权限</h4>
          <select
            value={selectedUser || ''}
            onChange={(e) => setSelectedUser(parseInt(e.target.value))}
            className="user-select"
          >
            <option value="">选择用户</option>
            {allUsers.map(user => (
              <option key={user.id} value={user.id} disabled={hasPermission(user.id)}>
                {user.username}
              </option>
            ))}
          </select>
          <select
            value={selectedPermission}
            onChange={(e) => setSelectedPermission(parseInt(e.target.value))}
            className="permission-select"
          >
            <option value={0}>查看权限</option>
            <option value={1}>编辑权限</option>
          </select>
          <button
            onClick={handleAssignPermission}
            className="assign-btn"
            disabled={!selectedUser}
          >
            分配权限
          </button>
        </div>
      )}

      {/* 权限列表 */}
      <div className="permission-list">
        <h4>权限列表</h4>
        {permissions.length === 0 ? (
          <p className="no-permissions">暂无权限设置</p>
        ) : (
          permissions.map(permission => (
            <div key={permission.id} className="permission-item">
              <div className="permission-user">
                <span className="username">{getUsername(permission.userId)}</span>
                {permission.userId === currentUserId && (
                  <span className="current-user-tag">（我）</span>
                )}
              </div>
              <div className="permission-type">
                {isOwner && permission.userId !== currentUserId ? (
                  <select
                    value={permission.permissionType}
                    onChange={(e) => handleUpdatePermission(permission.userId, parseInt(e.target.value))}
                    className="permission-select"
                  >
                    <option value={0}>查看</option>
                    <option value={1}>编辑</option>
                  </select>
                ) : (
                  <span>{getPermissionText(permission.permissionType)}</span>
                )}
              </div>
              <div className="permission-actions">
                {isOwner && permission.userId !== currentUserId && (
                  <button
                    onClick={() => handleRemovePermission(permission.userId)}
                    className="remove-permission-btn"
                  >
                    移除
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

export default PermissionPanel;
