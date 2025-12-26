import { useNavigate } from 'react-router-dom';
import { userApi } from '../api/userApi';

/**
 * 认证相关 Hook
 * 处理登录检查和退出登录逻辑
 */
export const useAuth = () => {
  const navigate = useNavigate();

  /**
   * 检查是否登录
   * 验证本地存储中是否存在 token, userId, username
   */
  const checkLogin = (): boolean => {
    const token = localStorage.getItem('token');
    const userId = localStorage.getItem('userId');
    const username = localStorage.getItem('username');

    if (!token || !userId || !username) {
      localStorage.clear();
      const qs = new URLSearchParams(window.location.search);
      const noAuto = qs.get('noAutoLogin') === '1';
      const autoFlag = String(import.meta.env.VITE_AUTO_LOGIN || '').toLowerCase() === 'true';
      if (import.meta.env.DEV && autoFlag && !noAuto) {
        const port = window.location.port;
        const devUser = port === '5174' ? 'devuser2' : 'devuser1';
        const devPass = 'password123';
        (async () => {
          try {
            const loginRes = await userApi.login({ username: devUser, password: devPass });
            if (loginRes.code === 200) {
              return;
            }
            // 若登录失败则尝试注册后再登录
            await userApi.register({ username: devUser, password: devPass });
            await userApi.login({ username: devUser, password: devPass });
          } catch {
            // 回退到登录页
            navigate('/login');
          }
        })();
        // 返回false，等待自动登录完成后由页面再次触发
        return false;
      } else {
        navigate('/login');
        return false;
      }
    }
    return true;
  };

  /**
   * 退出登录
   * 清除本地存储的认证信息并跳转回登录页
   */
  const handleLogout = async () => {
    try {
      // 调用后端登出接口（可选）
      // await userApi.logout(); 
    } catch (error) {
      console.error('登出失败', error);
    } finally {
      localStorage.removeItem('token');
      localStorage.removeItem('userId');
      localStorage.removeItem('username');
      navigate('/login');
    }
  };

  return { checkLogin, handleLogout };
};
