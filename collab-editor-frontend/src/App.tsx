import React, { Suspense, useEffect, useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { errorLogger } from './utils/errorLogger';
import MonitorDashboard from './pages/MonitorDashboard';
import { userApi } from './api/userApi';
import { documentApi, versionApi } from './api/documentApi';

// 使用React.lazy实现组件懒加载
const Login = React.lazy(() => import('./components/Login'));
const Register = React.lazy(() => import('./components/Register'));
const DocumentList = React.lazy(() => import('./components/DocumentList'));
const Editor = React.lazy(() => import('./components/Editor'));
const Profile = React.lazy(() => import('./components/Profile'));
const PasswordReset = React.lazy(() => import('./components/PasswordReset'));
const CodePlayground = React.lazy(() => import('./components/CodePlayground'));
const AdminPanel = React.lazy(() => import('./components/AdminPanel'));

const App: React.FC = () => {
  // 使用localStorage模拟认证状态
  const [isAuthenticated, setIsAuthenticated] = useState(!!localStorage.getItem('token'));
  const [userRole, setUserRole] = useState(localStorage.getItem('role') || 'editor');

  // 深色模式已集成到主题中，无需单独切换函数

  // 监听认证状态变化
  useEffect(() => {
    // 初始检查
    if (!isAuthenticated) {
      const hasToken = !!localStorage.getItem('token');
      if (hasToken) {
        setIsAuthenticated(true);
        setUserRole(localStorage.getItem('role') || 'editor');
      }
    }

    const checkAuth = () => {
      setIsAuthenticated(!!localStorage.getItem('token'));
      setUserRole(localStorage.getItem('role') || 'editor');
    };

    // 监听localStorage变化
    window.addEventListener('storage', checkAuth);
    return () => window.removeEventListener('storage', checkAuth);
  }, [isAuthenticated, userRole]);

  // 根据路由变化同步认证状态，解决登录后立即跳转受限路由仍为未认证的问题
  const AuthSync: React.FC = () => {
    const location = useLocation();
    useEffect(() => {
      setIsAuthenticated(!!localStorage.getItem('token'));
      setUserRole(localStorage.getItem('role') || 'editor');
    }, [location]);
    return null;
  };

  const theme = createTheme({
    palette: {
      mode: 'dark',
      primary: {
        main: '#5E6AD2', // Linear-like Blurple
        light: '#7A85DE',
        dark: '#4550A8',
        contrastText: '#ffffff',
      },
      secondary: {
        main: '#C3688B', // Linear-like Pinkish
        light: '#D489A5',
        dark: '#9F4A6A',
        contrastText: '#ffffff',
      },
      background: {
        default: '#08090A', // Linear Dark Background
        paper: '#131416', // Slightly lighter for panels
      },
      text: {
        primary: '#EBEBF5', // High emphasis
        secondary: 'rgba(235, 235, 245, 0.6)', // Medium emphasis
      },
      divider: 'rgba(255, 255, 255, 0.08)',
      action: {
        hover: 'rgba(255, 255, 255, 0.04)',
        selected: 'rgba(255, 255, 255, 0.08)',
      },
    },
    shape: {
      borderRadius: 6, // Slightly rounded, professional look
    },
    typography: {
      fontFamily: '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
      h1: { fontWeight: 600, letterSpacing: '-0.02em' },
      h2: { fontWeight: 600, letterSpacing: '-0.02em' },
      h3: { fontWeight: 600, letterSpacing: '-0.01em' },
      h4: { fontWeight: 500, letterSpacing: '-0.01em' },
      h5: { fontWeight: 500 },
      h6: { fontWeight: 500 },
      button: {
        fontWeight: 500,
        textTransform: 'none',
        letterSpacing: '0.01em',
      },
      body1: {
        letterSpacing: '-0.01em',
      },
      body2: {
        letterSpacing: '-0.005em',
      },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: {
            backgroundColor: '#08090A',
            // Subtle noise texture + faint gradient for depth
            backgroundImage: `
              radial-gradient(at 0% 0%, rgba(94, 106, 210, 0.08) 0px, transparent 50%), 
              radial-gradient(at 100% 0%, rgba(195, 104, 139, 0.05) 0px, transparent 50%)
            `,
            minHeight: '100vh',
            fontFeatureSettings: '"cv11", "ss01"',
            '&::-webkit-scrollbar': {
              width: '10px',
              height: '10px',
            },
            '&::-webkit-scrollbar-track': {
              background: '#08090A',
            },
            '&::-webkit-scrollbar-thumb': {
              background: '#2D2E33',
              borderRadius: '5px',
              border: '2px solid #08090A',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: '#45464F',
            },
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: '6px',
            padding: '6px 12px',
            fontSize: '0.9rem',
            transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            '&:hover': {
              transform: 'translateY(-1px)',
            },
          },
          contained: {
            boxShadow: '0 1px 2px rgba(0,0,0,0.2)',
            border: '1px solid rgba(255,255,255,0.1)',
            '&:hover': {
              boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
            },
          },
          outlined: {
            borderColor: 'rgba(255,255,255,0.15)',
            color: '#EBEBF5',
            '&:hover': {
              borderColor: '#EBEBF5',
              backgroundColor: 'rgba(255,255,255,0.04)',
            },
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            backgroundImage: 'none',
            backgroundColor: '#131416',
            border: '1px solid rgba(255,255,255,0.08)',
            boxShadow: '0 4px 20px rgba(0,0,0,0.2)',
          },
          elevation1: {
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          },
          elevation4: {
            // For modals/popups
            boxShadow: '0 8px 30px rgba(0,0,0,0.4)',
            border: '1px solid rgba(255,255,255,0.1)',
          }
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: '8px',
            backgroundColor: '#131416',
            border: '1px solid rgba(255,255,255,0.08)',
            transition: 'all 0.2s ease',
            '&:hover': {
              borderColor: 'rgba(255,255,255,0.15)',
              transform: 'translateY(-2px)',
              boxShadow: '0 8px 20px rgba(0,0,0,0.3)',
            },
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundColor: 'rgba(8, 9, 10, 0.8)',
            borderBottom: '1px solid rgba(255,255,255,0.08)',
            backdropFilter: 'blur(16px)',
            boxShadow: 'none',
          },
        },
      },
      MuiTextField: {
        styleOverrides: {
          root: {
            '& .MuiOutlinedInput-root': {
              borderRadius: '6px',
              backgroundColor: 'rgba(255,255,255,0.03)',
              transition: 'all 0.2s',
              '& fieldset': {
                borderColor: 'rgba(255,255,255,0.1)',
                transition: 'border-color 0.2s',
              },
              '&:hover': {
                backgroundColor: 'rgba(255,255,255,0.05)',
              },
              '&:hover fieldset': {
                borderColor: 'rgba(255,255,255,0.2)',
              },
              '&.Mui-focused': {
                backgroundColor: 'rgba(255,255,255,0.05)',
              },
              '&.Mui-focused fieldset': {
                borderColor: '#5E6AD2', // Primary
                borderWidth: '1px',
                boxShadow: '0 0 0 2px rgba(94, 106, 210, 0.2)',
              },
            },
            '& .MuiInputLabel-root': {
              color: 'rgba(235, 235, 245, 0.6)',
            },
            '& .MuiInputLabel-root.Mui-focused': {
              color: '#5E6AD2',
            },
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: {
            borderRadius: '4px',
            fontWeight: 500,
            border: '1px solid transparent',
          },
          filled: {
            backgroundColor: 'rgba(255,255,255,0.08)',
            '&:hover': {
              backgroundColor: 'rgba(255,255,255,0.12)',
            },
          },
          outlined: {
            borderColor: 'rgba(255,255,255,0.15)',
            '&:hover': {
              borderColor: 'rgba(255,255,255,0.3)',
              backgroundColor: 'transparent',
            },
          },
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: {
            backgroundColor: '#18191B',
            border: '1px solid rgba(255,255,255,0.1)',
            boxShadow: '0 20px 40px rgba(0,0,0,0.4)',
            borderRadius: '10px',
          },
        },
      },
      MuiMenu: {
        styleOverrides: {
          paper: {
            backgroundColor: '#18191B',
            border: '1px solid rgba(255,255,255,0.1)',
            boxShadow: '0 10px 30px rgba(0,0,0,0.4)',
            borderRadius: '8px',
          },
        },
      },
    },
  });

  useEffect(() => {
    // 发送localStorage中保存的未发送日志
    errorLogger.sendLocalStorageLogs();

    // 组件卸载时清理资源
    return () => {
      errorLogger.destroy();
    };
  }, []);

  useEffect(() => {
    const run = async () => {
      if (!import.meta.env.DEV) return;
      const s = new URLSearchParams(window.location.search);
      if (s.get('runPreviewFlow') !== '1') return;
      try {
        const username = 'preview_user';
        const password = 'preview_pass123';
        let login = await userApi.login({ username, password });
        if (!(login?.code === 200)) {
          await userApi.register({ username, password, email: 'preview_user@example.com' });
          login = await userApi.login({ username, password });
        }
        const create = await documentApi.create('预览测试文档', 'demo', 'test', '初始内容：Hello');
        const docId = create?.data?.data?.id ?? create?.data?.id ?? create?.data?.data?.docId;
        if (!docId) return;
        await documentApi.saveContent(docId, '版本1内容：Hello World');
        await versionApi.createVersion(docId, '版本1内容：Hello World', '版本1', '预览创建');
        await documentApi.saveContent(docId, '版本2内容：你好，世界');
        await versionApi.createVersion(docId, '版本2内容：你好，世界', '版本2', '预览创建');
        const listResp = await versionApi.getVersions(docId);
        const list = Array.isArray(listResp?.data?.data) ? listResp.data.data : [];
        if (list.length === 0) return;
        const ordered = [...list].sort((a: any, b: any) => new Date(a.createTime).getTime() - new Date(b.createTime).getTime());
        const v1Id = ordered[0]?.id;
        if (v1Id) {
          await versionApi.rollbackToVersion(docId, v1Id);
        }
      } catch { }
    };
    run();
  }, [isAuthenticated]);

  // 检查用户是否有权限访问监控仪表板（临时允许所有人访问）
  // const canAccessMonitor = true;

  return (
    <Router>
      <AuthSync />
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Suspense fallback={<div className="loading">加载中...</div>}>
          <Routes>
            <Route path="/" element={isAuthenticated ? <DocumentList /> : <Navigate to="/login" />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/reset-password" element={<PasswordReset />} />
            <Route path="/documents" element={<DocumentList />} />
            <Route path="/editor/:docId" element={isAuthenticated ? <Editor /> : <Navigate to="/login" />} />
            <Route path="/profile" element={isAuthenticated ? <Profile /> : <Navigate to="/login" />} />
            <Route path="/playground" element={isAuthenticated ? <CodePlayground /> : <Navigate to="/login" />} />
            <Route path="/monitor" element={isAuthenticated ? <MonitorDashboard /> : <Navigate to="/" />} />
            <Route path="/admin" element={isAuthenticated ? <AdminPanel /> : <Navigate to="/" />} />
          </Routes>
        </Suspense>
      </ThemeProvider>
    </Router>
  );
};

export default App;
