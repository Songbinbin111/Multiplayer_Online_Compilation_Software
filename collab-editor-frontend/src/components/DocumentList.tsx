import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { documentApi, userApi } from '../api';
import NotificationPanel from './NotificationPanel';

interface Document {
  id: number;
  title: string;
  createTime: string;
  updateTime: string;
}

const DocumentList: React.FC = () => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [newDocTitle, setNewDocTitle] = useState('');
  const navigate = useNavigate();

  // 检查是否登录
  const checkLogin = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return false;
    }
    return true;
  };

  // 获取文档列表
  const fetchDocuments = async () => {
    if (!checkLogin()) return;

    try {
      setLoading(true);
      const response = await documentApi.getList();
      const data = response.data;
      setDocuments(data);
    } catch (error) {
      console.error('获取文档列表失败:', error);
    } finally {
      setLoading(false);
    }
  };

  // 创建新文档
  const handleCreateDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDocTitle.trim()) return;

    try {
      await documentApi.create(newDocTitle.trim());
      setNewDocTitle('');
      fetchDocuments(); // 刷新文档列表
    } catch (error) {
      console.error('创建文档失败:', error);
    }
  };

  // 编辑文档
  const handleEditDocument = (docId: number) => {
    navigate(`/editor/${docId}`);
  };

  // 退出登录
  const handleLogout = () => {
    userApi.logout();
    navigate('/login');
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  return (
    <div className="document-list-container">
      <header className="header">
        <h1>在线协作编辑器</h1>
        <div className="user-info">
          <NotificationPanel currentUserId={parseInt(localStorage.getItem('userId') || '0')} />
          <span>欢迎，{localStorage.getItem('username')}</span>
          <button onClick={handleLogout} className="logout-button">退出</button>
        </div>
      </header>

      <div className="content">
        <div className="create-doc-form">
          <h2>我的文档</h2>
          <form onSubmit={handleCreateDocument}>
            <input
              type="text"
              placeholder="输入文档标题"
              value={newDocTitle}
              onChange={(e) => setNewDocTitle(e.target.value)}
              className="doc-title-input"
              required
            />
            <button type="submit" className="create-doc-button">创建文档</button>
          </form>
        </div>

        <div className="doc-list">
          {loading ? (
            <div className="loading">加载中...</div>
          ) : (
            documents.map((doc) => (
              <div
                key={doc.id}
                className="doc-item"
                onClick={() => handleEditDocument(doc.id)}
              >
                <h3 className="doc-title">{doc.title}</h3>
                <div className="doc-meta">
                  <span>创建时间: {new Date(doc.createTime).toLocaleString()}</span>
                  <span>更新时间: {new Date(doc.updateTime).toLocaleString()}</span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};

export default DocumentList;