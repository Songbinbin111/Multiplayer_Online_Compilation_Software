import React, { useState, useEffect } from 'react';
import { versionApi } from '../api/documentApi';

// 版本信息接口
interface Version {
  id: number;
  docId: number;
  versionName?: string;
  content: string;
  description?: string;
  createTime: string;
  createBy: number;
}

interface VersionControlProps {
  docId: number;
  onVersionSelect: (version: Version) => void;
  onClose: () => void;
}

const VersionControl: React.FC<VersionControlProps> = ({ docId, onVersionSelect, onClose }) => {
  const [versions, setVersions] = useState<Version[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 获取版本列表
  const fetchVersions = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await versionApi.getVersions(docId);
      const responseData = response.data || response;
      if (responseData && Array.isArray(responseData)) {
        setVersions(responseData);
      } else {
        setError('获取版本列表失败');
      }
    } catch (err) {
      setError('网络错误，获取版本列表失败');
      console.error('获取版本列表失败:', err);
    } finally {
      setLoading(false);
    }
  };

  // 回滚到指定版本
  const handleRollback = async (versionId: number) => {
    try {
      await versionApi.rollbackToVersion(docId, versionId);
      alert('版本回滚成功');
      fetchVersions(); // 刷新版本列表
    } catch (err: any) {
      alert(`回滚失败: ${err.response?.data?.message || '网络错误'}`);
      console.error('版本回滚失败:', err);
    }
  };

  // 查看版本内容
  const handleViewVersion = async (versionId: number) => {
    try {
      const response = await versionApi.getVersion(versionId);
      const responseData = response.data || response;
      if (responseData) {
        onVersionSelect(responseData);
      } else {
        alert('获取版本内容失败');
      }
    } catch (err) {
      alert('网络错误，获取版本内容失败');
      console.error('获取版本内容失败:', err);
    }
  };

  // 组件挂载时获取版本列表
  useEffect(() => {
    fetchVersions();
  }, [docId]);

  return (
    <div className="version-control-container">
      <div className="version-control-header">
        <h3>文档版本历史</h3>
        <button className="close-btn" onClick={onClose}>×</button>
      </div>

      <div className="version-control-content">
        {loading ? (
          <div className="loading">加载版本列表中...</div>
        ) : error ? (
          <div className="error">{error}</div>
        ) : versions.length === 0 ? (
          <div className="empty">暂无版本记录</div>
        ) : (
          <ul className="version-list">
            {versions.map((version) => (
              <li key={version.id} className="version-item">
                <div className="version-info">
                  <div className="version-name">
                    {version.versionName || `版本 ${version.id}`}
                  </div>
                  <div className="version-meta">
                    <span className="create-time">
                      创建时间: {new Date(version.createTime).toLocaleString()}
                    </span>
                    <span className="create-by">
                      创建人: {version.createBy}
                    </span>
                  </div>
                  {version.description && (
                    <div className="version-description">
                      {version.description}
                    </div>
                  )}
                </div>
                <div className="version-actions">
                  <button
                    className="view-btn"
                    onClick={() => handleViewVersion(version.id)}
                  >
                    查看
                  </button>
                  <button
                    className="rollback-btn"
                    onClick={() => handleRollback(version.id)}
                  >
                    回滚
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <style>{`
        .version-control-container {
          position: fixed;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          background: white;
          border-radius: 8px;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
          width: 80%;
          max-width: 800px;
          max-height: 80vh;
          z-index: 1000;
          display: flex;
          flex-direction: column;
        }
        
        .version-control-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16px 24px;
          border-bottom: 1px solid #e0e0e0;
        }
        
        .version-control-header h3 {
          margin: 0;
          font-size: 18px;
          color: #333;
        }
        
        .close-btn {
          background: none;
          border: none;
          font-size: 24px;
          cursor: pointer;
          color: #666;
          padding: 0;
          width: 30px;
          height: 30px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 4px;
          transition: background-color 0.2s;
        }
        
        .close-btn:hover {
          background-color: #f5f5f5;
        }
        
        .version-control-content {
          padding: 20px;
          overflow-y: auto;
          flex: 1;
        }
        
        .loading, .error, .empty {
          text-align: center;
          padding: 40px 0;
          color: #666;
        }
        
        .version-list {
          list-style: none;
          padding: 0;
          margin: 0;
        }
        
        .version-item {
          border: 1px solid #e0e0e0;
          border-radius: 6px;
          margin-bottom: 12px;
          padding: 16px;
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          transition: box-shadow 0.2s;
        }
        
        .version-item:hover {
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
        }
        
        .version-info {
          flex: 1;
        }
        
        .version-name {
          font-weight: 600;
          color: #333;
          margin-bottom: 8px;
        }
        
        .version-meta {
          font-size: 14px;
          color: #666;
          margin-bottom: 8px;
          display: flex;
          gap: 16px;
        }
        
        .version-description {
          font-size: 14px;
          color: #888;
          margin-top: 8px;
          line-height: 1.4;
        }
        
        .version-actions {
          display: flex;
          gap: 8px;
          flex-shrink: 0;
        }
        
        .view-btn, .rollback-btn {
          padding: 6px 12px;
          border: none;
          border-radius: 4px;
          cursor: pointer;
          font-size: 14px;
          transition: background-color 0.2s;
        }
        
        .view-btn {
          background-color: #e3f2fd;
          color: #1976d2;
        }
        
        .view-btn:hover {
          background-color: #bbdefb;
        }
        
        .rollback-btn {
          background-color: #fff3e0;
          color: #f57c00;
        }
        
        .rollback-btn:hover {
          background-color: #ffe0b2;
        }
      `}</style>
    </div>
  );
};

export default VersionControl;
