import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../api';

const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const response = await userApi.login({ username, password });
      if (response.data && response.data.code === 200) {
        // 跳转到文档列表页面
        navigate('/documents');
      } else {
        setError(response.data && response.data.message ? response.data.message : '登录失败');
      }
    } catch (err) {
      setError('登录失败，请检查网络或用户名密码');
    }
  };

  return (
    <div className="auth-container">
      <form className="auth-form" onSubmit={handleLogin}>
        <h2>登录</h2>
        {error && <div className="error-message">{error}</div>}
        <div className="form-group">
          <label htmlFor="username">用户名</label>
          <input
            type="text"
            id="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />
        </div>
        <div className="form-group">
          <label htmlFor="password">密码</label>
          <input
            type="password"
            id="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit" className="submit-button">登录</button>
        <p className="switch-form">
          还没有账号？<a href="/register">去注册</a>
        </p>
      </form>
    </div>
  );
};

export default Login;