import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../api';

const Register: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    // 验证密码一致性
    if (password !== confirmPassword) {
      setError('两次输入的密码不一致');
      return;
    }

    try {
      console.log('注册请求数据:', { username, password });
      const response = await userApi.register({ username, password });
      console.log('注册响应:', response);

      // 检查业务逻辑状态码
      if (response.data && response.data.code === 200) {
        // 注册成功，跳转到登录页面
        navigate('/login');
      } else {
        setError(response.data && response.data.message ? response.data.message : '注册失败');
        console.log('注册失败原因:', response.data?.message);
      }
    } catch (err) {
      console.error('注册请求异常:', err);
      setError('注册失败，请检查网络或用户名是否已存在');
    }
  };

  return (
    <div className="auth-container">
      <form className="auth-form" onSubmit={handleRegister}>
        <h2>注册</h2>
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
        <div className="form-group">
          <label htmlFor="confirmPassword">确认密码</label>
          <input
            type="password"
            id="confirmPassword"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
          />
        </div>
        <button type="submit" className="submit-button">注册</button>
        <p className="switch-form">
          已有账号？<a href="/login">去登录</a>
        </p>
      </form>
    </div>
  );
};

export default Register;