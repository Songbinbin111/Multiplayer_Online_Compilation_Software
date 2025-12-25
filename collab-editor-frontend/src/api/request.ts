import axios from 'axios';

// 创建axios实例
const api = axios.create({
  // Docker环境下使用容器名称访问后端，开发环境使用localhost
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 10000
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    // 保留完整的响应结构，不做统一处理
    return response;
  },
  async error => {
    // 处理响应错误
    const originalRequest = error.config;
    
    // 如果是401错误且不是刷新令牌的请求
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // 尝试刷新令牌，传递当前的Authorization头
        const refreshResponse = await api.post('/api/refresh-token', {}, {
          headers: {
            Authorization: originalRequest.headers['Authorization']
          }
        });
        
        if (refreshResponse.data.code === 200 && refreshResponse.data.data?.token) {
          // 更新localStorage中的令牌
          localStorage.setItem('token', refreshResponse.data.data.token);
          
          // 更新请求头中的令牌
          api.defaults.headers.common['Authorization'] = `Bearer ${refreshResponse.data.data.token}`;
          originalRequest.headers['Authorization'] = `Bearer ${refreshResponse.data.data.token}`;
          
          // 重新发起原始请求
          return api(originalRequest);
        }
      } catch (refreshError) {
        // 刷新令牌失败，清除localStorage并重定向到登录页面
        localStorage.removeItem('token');
        localStorage.removeItem('username');
        localStorage.removeItem('userId');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export { api };
export default api;
