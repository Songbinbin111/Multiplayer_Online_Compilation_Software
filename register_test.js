const axios = require('axios');

// 创建axios实例，与前端配置一致
const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000
});

// 注册新用户
const registerNewUser = async () => {
  const newUser = {
    username: 'testuser' + Date.now(), // 使用时间戳确保用户名唯一
    password: 'password123'
  };

  console.log('正在注册新用户:', newUser.username);

  try {
    const response = await api.post('/api/register', newUser);
    console.log('注册成功:', response.data);
    return response.data;
  } catch (error) {
    console.error('注册失败:', error.response ? error.response.data : error.message);
    return null;
  }
};

// 执行注册
registerNewUser();