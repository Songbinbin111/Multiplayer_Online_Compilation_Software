import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { documentApi, userApi, versionApi } from '../api/index';
import NotificationPanel from './NotificationPanel';
import NotificationSettingModal from './NotificationSettingModal';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  Container,
  Grid,
  Card,
  CardActionArea,
  TextField,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Box,
  Chip,
  InputAdornment,
  Paper,
  Stack,
  Collapse,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  IconButton,
  Divider
} from '@mui/material';
import {
  Add as AddIcon,
  Search as SearchIcon,
  FilterList as FilterListIcon,
  Logout as LogoutIcon,
  Person as PersonIcon,
  Dashboard as DashboardIcon,
  Description as DescriptionIcon,
  Delete as DeleteIcon,
  Code as CodeIcon,
  Tune as TuneIcon,
  ExpandLess as ExpandLessIcon,
  AdminPanelSettings as AdminPanelSettingsIcon
} from '@mui/icons-material';

interface Document {
  id: number;
  title: string;
  category?: string;
  createTime: string;
  updateTime: string;
  ownerId?: number;
  ownerName?: string;
}

// 预定义的模板列表
const TEMPLATE_OPTIONS = [
  {
    id: 'empty',
    name: '空白文档',
    content: ''
  },
  {
    id: 'meeting-minutes',
    name: '会议纪要',
    content: '<h1>会议纪要</h1><p><strong>日期：</strong>2023-XX-XX</p><p><strong>参会人：</strong></p><h2>会议主题</h2><p>请输入会议主题...</p><h2>讨论内容</h2><ul><li>事项1</li><li>事项2</li></ul><h2>后续行动</h2><ul><li>[ ] 任务1 (负责人: )</li></ul>'
  },
  {
    id: 'project-plan',
    name: '项目计划',
    content: '<h1>项目计划书</h1><h2>1. 项目背景</h2><p>...</p><h2>2. 项目目标</h2><p>...</p><h2>3. 里程碑</h2><ul><li>阶段一：需求分析 (2023-XX-XX)</li><li>阶段二：开发实现 (2023-XX-XX)</li></ul>'
  },
  {
    id: 'daily-report',
    name: '工作日报',
    content: '<h1>工作日报</h1><p><strong>日期：</strong>2023-XX-XX</p><h2>今日工作内容</h2><ol><li>完成...</li><li>修复...</li></ol><h2>遇到的问题</h2><p>无</p><h2>明日计划</h2><ol><li>继续...</li></ol>'
  }
];

const DocumentList: React.FC = () => {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [loading, setLoading] = useState(true);
  const [newDocTitle, setNewDocTitle] = useState('');
  const [newDocCategory, setNewDocCategory] = useState('');
  const [newDocTags, setNewDocTags] = useState('');
  const [selectedTemplate, setSelectedTemplate] = useState(TEMPLATE_OPTIONS[0].id);
  const navigate = useNavigate();
  // 搜索相关状态
  const [searchKeyword, setSearchKeyword] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [sortField, setSortField] = useState('updateTime');
  const [sortOrder, setSortOrder] = useState('desc');
  const [searchScope, setSearchScope] = useState<'title' | 'title_exact' | 'content' | 'all'>('title');
  const [showAdvancedSearch, setShowAdvancedSearch] = useState(false);
  const [searchResults, setSearchResults] = useState<Document[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  // 分类相关状态
  const [categories, setCategories] = useState<string[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [isAdmin, setIsAdmin] = useState(false);
  const [isNotificationSettingOpen, setIsNotificationSettingOpen] = useState(false);
  // 删除确认对话框状态
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [documentToDelete, setDocumentToDelete] = useState<{ id: number, title: string } | null>(null);
  const [userMap, setUserMap] = useState<Record<number, string>>({});
  const [lastUpdaterMap, setLastUpdaterMap] = useState<Record<number, string>>({});

  // 检查是否登录
  const checkLogin = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return false;
    }
    return true;
  };

  /**
   * 获取所有文档列表
   * 调用后端 API 获取最新的文档数据
   */
  const fetchDocuments = async () => {
    if (!checkLogin()) return;

    try {
      setLoading(true);
      const response = await documentApi.getList();
      const data = response.data;

      if (data.code !== 200) {
        console.error('获取文档列表失败:', data.message);
        // Alert only if it's not a common auth error (which redirects)
        if (data.code !== 401) {
          // alert(`获取文档列表失败: ${data.message}`);
        }
        setDocuments([]);
        return;
      }

      // 后端返回的是Result对象，文档列表在data.data中
      const documents = data && data.data ? data.data : [];
      // 确保documents始终是一个数组
      setDocuments(Array.isArray(documents) ? documents : []);
      setSearchResults([]); // 清除搜索结果
      setSelectedCategory(''); // 清除选中分类，因为这是获取所有文档
    } catch (error) {
      console.error('获取文档列表失败:', error);
      // 发生错误时设置为空数组
      setDocuments([]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 获取文档分类列表
   * 用于侧边栏筛选展示
   */
  const fetchCategories = async () => {
    if (!checkLogin()) return;

    try {
      // 直接使用api.get调用，避免documentApi.getCategories的问题
      const apiResponse = await fetch('http://localhost:8080/api/doc/categories', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
          'Content-Type': 'application/json'
        }
      });

      const data = await apiResponse.json();
      // 后端返回的是Result对象，分类列表在data.data中
      const categories = data && data.data ? data.data : [];
      // 确保categories始终是一个数组
      setCategories(Array.isArray(categories) ? categories : []);
    } catch (error) {
      console.error('获取分类列表失败:', error);
      // 发生错误时设置为空数组
      setCategories([]);
    }
  };

  /**
   * 根据分类筛选文档
   * @param category 选中的分类名称
   */
  const fetchDocumentsByCategory = async (category: string) => {
    if (!checkLogin()) return;

    try {
      setLoading(true);
      setSelectedCategory(category);
      const response = await documentApi.getListByCategory(category);
      const data = response.data;
      // 后端返回的是Result对象，文档列表在data.data中
      const documents = data && data.data ? data.data : [];
      // 确保documents始终是一个数组
      setDocuments(Array.isArray(documents) ? documents : []);
      setSearchResults([]); // 清除搜索结果
    } catch (error) {
      console.error('按分类获取文档列表失败:', error);
      // 发生错误时设置为空数组
      setDocuments([]);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 执行文档搜索
   * 支持按关键词、时间范围、标签、作者等多维度搜索
   */
  const searchDocuments = async () => {
    setIsSearching(true);
    try {
      const response = await documentApi.search(
        searchKeyword || undefined,
        startDate || undefined,
        endDate || undefined,
        sortField,
        sortOrder,
        selectedCategory || undefined,
        searchScope
      );
      const data = response.data;
      // 后端返回的是Result对象，搜索结果在data.data中
      const searchResults = data && data.data ? data.data : [];
      // 确保searchResults始终是一个数组
      setSearchResults(Array.isArray(searchResults) ? searchResults : []);
    } catch (error) {
      console.error('搜索文档失败:', error);
      // 发生错误时设置为空数组
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  };

  // 重置搜索
  const resetSearch = () => {
    setSearchKeyword('');
    setStartDate('');
    setEndDate('');
    setSortField('updateTime');
    setSortOrder('desc');
    setSearchResults([]);
    fetchDocuments();
  };

  // 创建新文档
  const handleCreateDocument = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newDocTitle.trim()) return;

    try {
      const templateContent = TEMPLATE_OPTIONS.find(t => t.id === selectedTemplate)?.content || '';

      const response = await documentApi.create(
        newDocTitle.trim(),
        newDocCategory || undefined,
        newDocTags || undefined, // tags
        templateContent // content
      );

      if (response.data.code === 200) {
        // alert('创建文档成功'); // Optional: show success message
        setNewDocTitle('');
        setNewDocCategory('');
        setNewDocTags('');
        setSelectedTemplate(TEMPLATE_OPTIONS[0].id);

        // 强制刷新列表
        await fetchDocuments();
      } else {
        alert(`创建文档失败: ${response.data.message || '未知错误'}`);
      }
    } catch (error: any) {
      console.error('创建文档失败:', error);
      alert(`创建文档失败: ${error.message || '网络错误'}`);
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

  // 处理删除点击
  const handleDeleteClick = (e: React.MouseEvent, doc: Document) => {
    e.stopPropagation(); // 防止触发卡片点击跳转
    setDocumentToDelete({ id: doc.id, title: doc.title });
    setDeleteDialogOpen(true);
  };

  // 确认删除文档
  const handleConfirmDelete = async () => {
    if (!documentToDelete) return;

    try {
      const response = await documentApi.delete(documentToDelete.id);
      if (response.data.code === 200) {
        // alert('删除成功');
        // 刷新列表
        fetchDocuments();
      } else {
        alert(`删除失败: ${response.data.message}`);
      }
    } catch (error: any) {
      console.error('删除文档失败:', error);
      alert('删除文档失败，请稍后重试');
    } finally {
      setDeleteDialogOpen(false);
      setDocumentToDelete(null);
    }
  };

  useEffect(() => {
    fetchDocuments();
    fetchCategories();

    (async () => {
      try {
        const usersRes = await userApi.getList();
        const users = usersRes && usersRes.data ? usersRes.data : [];
        const map: Record<number, string> = {};
        (Array.isArray(users) ? users : []).forEach((u: any) => {
          if (typeof u.id === 'number') {
            map[u.id] = u.nickname || u.username || `用户${u.id}`;
          }
        });
        setUserMap(map);
      } catch { }
    })();

    // Check user role from localStorage first
    const role = localStorage.getItem('role');
    if (role === 'admin') {
      setIsAdmin(true);
    }

    // Verify with backend to be sure (and sync if needed)
    const userId = parseInt(localStorage.getItem('userId') || '0');
    if (userId) {
      userApi.getProfile(userId).then(res => {
        if (res.code === 200 && res.data) {
          const currentRole = res.data.role;
          if (currentRole === 'admin') {
            setIsAdmin(true);
            localStorage.setItem('role', 'admin');
          } else {
            setIsAdmin(false);
            if (localStorage.getItem('role') === 'admin') {
              localStorage.setItem('role', currentRole || 'editor');
            }
          }
        }
      }).catch(err => console.error('Failed to fetch user profile:', err));
    }
  }, []);

  // Prepare display documents
  const displayDocuments = searchResults.length > 0 ? searchResults : documents;

  // 根据最新版本的创建者计算最近更新者
  useEffect(() => {
    const docs = displayDocuments;
    const needFetchIds = docs
      .map(d => d.id)
      .filter(id => !(id in lastUpdaterMap) || lastUpdaterMap[id] === '未知');

    if (needFetchIds.length === 0) return;

    (async () => {
      const newMap: Record<number, string> = { ...lastUpdaterMap };
      for (const docId of needFetchIds) {
        try {
          const res = await versionApi.getVersions(docId);
          const list = res && res.data && res.data.data ? res.data.data : [];
          if (Array.isArray(list) && list.length > 0) {
            const latest = list[0];
            const uid: number | undefined = (latest as any).createdBy ?? (latest as any).createBy;
            if (typeof uid === 'number') {
              newMap[docId] = userMap[uid] || `用户${uid}`;
            } else {
              const docObj = docs.find(d => d.id === docId);
              const ownerName = docObj
                ? (docObj.ownerId && userMap[docObj.ownerId] ? userMap[docObj.ownerId] : (docObj.ownerName || '未知'))
                : '未知';
              newMap[docId] = ownerName;
            }
          } else {
            const docObj = docs.find(d => d.id === docId);
            const ownerName = docObj
              ? (docObj.ownerId && userMap[docObj.ownerId] ? userMap[docObj.ownerId] : (docObj.ownerName || '未知'))
              : '未知';
            newMap[docId] = ownerName;
          }
        } catch {
          const docObj = docs.find(d => d.id === docId);
          const ownerName = docObj
            ? (docObj.ownerId && userMap[docObj.ownerId] ? userMap[docObj.ownerId] : (docObj.ownerName || '未知'))
            : '未知';
          newMap[docId] = ownerName;
        }
      }
      setLastUpdaterMap(newMap);
    })();
  }, [documents, searchResults, userMap]);

  return (
    <Box sx={{ flexGrow: 1, minHeight: '100vh', bgcolor: 'background.default' }}>
      {/* App Bar */}
      <AppBar position="static" color="transparent" elevation={0}>
        <Toolbar>
          <DescriptionIcon sx={{ mr: 2, color: 'primary.main' }} />
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 'bold', color: 'text.primary' }}>
            在线协作编辑器
          </Typography>

          <Stack direction="row" spacing={1} alignItems="center">
            <NotificationPanel currentUserId={parseInt(localStorage.getItem('userId') || '0')} />
            <Typography variant="body2" sx={{ mx: 2, display: { xs: 'none', sm: 'block' } }}>
              欢迎，{localStorage.getItem('username')}
            </Typography>
            {isAdmin && (
              <>
                <Button
                  variant="contained"
                  color="warning"
                  size="small"
                  startIcon={<DashboardIcon />}
                  onClick={() => navigate('/monitor')}
                  sx={{ display: { xs: 'none', sm: 'flex' } }}
                >
                  系统监控
                </Button>
                <Button
                  variant="contained"
                  color="secondary"
                  size="small"
                  startIcon={<AdminPanelSettingsIcon />}
                  onClick={() => navigate('/admin')}
                  sx={{ ml: 1, display: { xs: 'none', sm: 'flex' } }}
                >
                  管理后台
                </Button>
              </>
            )}
            <Button color="inherit" onClick={() => navigate('/profile')} startIcon={<PersonIcon />} sx={{ display: { xs: 'none', sm: 'flex' } }}>个人资料</Button>
            <Button
              variant="contained"
              startIcon={<CodeIcon />}
              onClick={() => navigate('/code')}
              sx={{
                background: 'linear-gradient(135deg, #5E6AD2 0%, #4550A8 100%)', // Linear blurple gradient
                boxShadow: '0 2px 4px rgba(0,0,0,0.2)',
                border: '1px solid rgba(255,255,255,0.1)',
                ml: 2,
                display: { xs: 'none', sm: 'flex' },
                transition: 'all 0.2s ease',
                '&:hover': {
                  boxShadow: '0 4px 12px rgba(94, 106, 210, 0.4)',
                  transform: 'translateY(-1px)',
                  border: '1px solid rgba(255,255,255,0.2)',
                }
              }}
            >
              在线编译器
            </Button>
            <IconButton color="inherit" onClick={handleLogout} title="退出登录" sx={{ ml: 1 }}>
              <LogoutIcon />
            </IconButton>
          </Stack>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
        <NotificationSettingModal
          isOpen={isNotificationSettingOpen}
          onClose={() => setIsNotificationSettingOpen(false)}
          userId={parseInt(localStorage.getItem('userId') || '0')}
        />

        {/* Create Doc Section（仅管理员可见） */}
        {isAdmin && (
        <Paper sx={{ p: 3, mb: 4, borderRadius: 2 }}>
          <Typography variant="h6" gutterBottom sx={{ fontWeight: 'bold', display: 'flex', alignItems: 'center' }}>
            <AddIcon sx={{ mr: 1, color: 'primary.main' }} /> 快速创建文档
          </Typography>
          <Box component="form" onSubmit={handleCreateDocument} sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'flex-start' }}>
            <TextField
              label="文档标题"
              value={newDocTitle}
              onChange={(e) => setNewDocTitle(e.target.value)}
              required
              size="small"
              sx={{ flexGrow: 1, minWidth: '200px' }}
            />
            <TextField
              label="分类"
              value={newDocCategory}
              onChange={(e) => setNewDocCategory(e.target.value)}
              size="small"
              sx={{ minWidth: '120px' }}
            />
            <TextField
              label="标签"
              value={newDocTags}
              onChange={(e) => setNewDocTags(e.target.value)}
              size="small"
              placeholder="逗号分隔"
              sx={{ minWidth: '150px' }}
            />
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>模板</InputLabel>
              <Select
                value={selectedTemplate}
                label="模板"
                onChange={(e) => setSelectedTemplate(e.target.value)}
              >
                {TEMPLATE_OPTIONS.map(option => (
                  <MenuItem key={option.id} value={option.id}>{option.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
            <Button type="submit" variant="contained" startIcon={<AddIcon />} sx={{ height: 40 }}>创建</Button>
          </Box>
        </Paper>
        )}

        {/* Filter and Search Combined Bar */}
        <Paper sx={{ p: 2, mb: 4, borderRadius: 2 }}>
          <Box sx={{ display: 'flex', flexDirection: { xs: 'column', md: 'row' }, alignItems: { xs: 'stretch', md: 'center' }, gap: 3 }}>
            {/* Categories Section */}
            <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 1.5, flex: { md: '0 0 auto' }, minWidth: { md: '300px' } }}>
              <Typography variant="subtitle2" sx={{ display: 'flex', alignItems: 'center', fontWeight: 600, color: 'text.secondary', mr: 1 }}>
                <FilterListIcon fontSize="small" sx={{ mr: 0.5 }} /> 分类:
              </Typography>
              <Chip
                label="全部"
                onClick={() => { setSelectedCategory(''); fetchDocuments(); }}
                color={selectedCategory === '' ? 'primary' : 'default'}
                variant={selectedCategory === '' ? 'filled' : 'outlined'}
                clickable
                size="small"
                sx={{ height: 28 }}
              />
              {categories.map((category, index) => (
                <Chip
                  key={index}
                  label={category}
                  onClick={() => fetchDocumentsByCategory(category)}
                  color={selectedCategory === category ? 'primary' : 'default'}
                  variant={selectedCategory === category ? 'filled' : 'outlined'}
                  clickable
                  size="small"
                  sx={{ height: 28 }}
                />
              ))}
            </Box>

            <Divider orientation="vertical" flexItem sx={{ display: { xs: 'none', md: 'block' } }} />
            <Divider sx={{ display: { xs: 'block', md: 'none' } }} />

            {/* Search Section */}
            <Box sx={{ flex: 1, display: 'flex', gap: 1.5 }}>
              <TextField
                fullWidth
                placeholder="搜索文档..."
                value={searchKeyword}
                onChange={(e) => setSearchKeyword(e.target.value)}
                size="small"
                InputProps={{
                  startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" color="action" /></InputAdornment>,
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    bgcolor: 'rgba(255,255,255,0.03)',
                    height: 36
                  }
                }}
              />
              <Button
                variant="contained"
                onClick={searchDocuments}
                disabled={isSearching}
                sx={{ height: 36, px: 3, whiteSpace: 'nowrap', minWidth: 'fit-content', flexShrink: 0 }}
              >
                搜索
              </Button>
              <Button
                variant="outlined"
                onClick={() => setShowAdvancedSearch(!showAdvancedSearch)}
                sx={{ height: 36, minWidth: 40, px: 1.5, flexShrink: 0 }}
                title="高级筛选"
              >
                {showAdvancedSearch ? <ExpandLessIcon /> : <TuneIcon fontSize="small" />}
              </Button>
              {(searchKeyword || startDate || endDate || searchResults.length > 0) && (
                <Button variant="text" onClick={resetSearch} size="small" sx={{ color: 'text.secondary', minWidth: 'auto', whiteSpace: 'nowrap', flexShrink: 0 }}>
                  重置
                </Button>
              )}
            </Box>
          </Box>

          {/* Advanced Search Collapse */}
          <Collapse in={showAdvancedSearch}>
            <Box sx={{ mt: 2, pt: 2, borderTop: '1px solid', borderColor: 'divider' }}>
              <Grid container spacing={2}>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <FormControl fullWidth size="small">
                    <InputLabel>排序</InputLabel>
                    <Select value={sortField} label="排序" onChange={(e) => setSortField(e.target.value)}>
                      <MenuItem value="updateTime">更新时间</MenuItem>
                      <MenuItem value="createTime">创建时间</MenuItem>
                      <MenuItem value="title">标题</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <FormControl fullWidth size="small">
                    <InputLabel>顺序</InputLabel>
                    <Select value={sortOrder} label="顺序" onChange={(e) => setSortOrder(e.target.value)}>
                      <MenuItem value="desc">降序</MenuItem>
                      <MenuItem value="asc">升序</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid size={{ xs: 6, sm: 3 }}>
                  <FormControl fullWidth size="small">
                    <InputLabel>范围</InputLabel>
                    <Select value={searchScope} label="范围" onChange={(e) => setSearchScope(e.target.value as any)}>
                      <MenuItem value="title">标题包含</MenuItem>
                      <MenuItem value="title_exact">标题精确</MenuItem>
                      <MenuItem value="content">全文</MenuItem>
                      <MenuItem value="all">标题或全文</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Stack direction="row" spacing={2} alignItems="center">
                    <Typography variant="caption" color="text.secondary" sx={{ minWidth: 60 }}>时间范围:</Typography>
                    <TextField
                      fullWidth
                      type="datetime-local"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      size="small"
                      sx={{
                        '& input': { fontSize: '0.8rem' },
                        '& input::-webkit-calendar-picker-indicator': {
                          filter: 'invert(1)',
                          cursor: 'pointer'
                        }
                      }}
                    />
                    <Typography variant="caption" color="text.secondary">-</Typography>
                    <TextField
                      fullWidth
                      type="datetime-local"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      size="small"
                      sx={{
                        '& input': { fontSize: '0.8rem' },
                        '& input::-webkit-calendar-picker-indicator': {
                          filter: 'invert(1)',
                          cursor: 'pointer'
                        }
                      }}
                    />
                  </Stack>
                </Grid>
              </Grid>
            </Box>
          </Collapse>
        </Paper>

        {/* Document List */}
        <Box>
          {isSearching && <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}><CircularProgress size={20} sx={{ mr: 1 }} /><Typography>搜索中...</Typography></Box>}

          {!isSearching && searchResults.length > 0 && (
            <Typography variant="h6" gutterBottom sx={{ mb: 2 }}>
              搜索结果 ({searchResults.length})
            </Typography>
          )}

          <Grid container spacing={3}>
            {loading && !isSearching ? (
              <Grid size={12} sx={{ display: 'flex', justifyContent: 'center', py: 5 }}>
                <CircularProgress />
              </Grid>
            ) : displayDocuments.length === 0 ? (
              <Grid size={12}>
                <Alert severity="info">暂无文档，请创建一个新文档</Alert>
              </Grid>
            ) : (
              displayDocuments.map((doc) => (
                <Grid size={{ xs: 12, sm: 6, md: 4, lg: 3 }} key={doc.id}>
                  <Card
                    sx={{
                      height: '100%',
                      display: 'flex',
                      flexDirection: 'column',
                      transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                      position: 'relative',
                      bgcolor: 'background.paper',
                      borderRadius: 2,
                      border: '1px solid',
                      borderColor: 'divider',
                      '&:hover': {
                        transform: 'translateY(-2px)',
                        boxShadow: '0 12px 24px -10px rgba(0, 0, 0, 0.3)',
                        borderColor: 'primary.main',
                        '& .delete-btn': {
                          opacity: 1,
                        }
                      }
                    }}
                  >
                    {isAdmin && (
                    <IconButton
                      className="delete-btn"
                      size="small"
                      onClick={(e) => handleDeleteClick(e, doc)}
                      sx={{
                        position: 'absolute',
                        top: 12,
                        right: 12,
                        zIndex: 2,
                        opacity: 0,
                        transition: 'opacity 0.2s',
                        color: 'text.secondary',
                        bgcolor: 'background.paper',
                        border: '1px solid',
                        borderColor: 'divider',
                        '&:hover': {
                          color: 'error.main',
                          bgcolor: 'background.paper',
                          borderColor: 'error.main',
                        }
                      }}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                    )}
                    <CardActionArea
                      onClick={() => handleEditDocument(doc.id)}
                      sx={{
                        flexGrow: 1,
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        p: 2.5,
                        height: '100%'
                      }}
                    >
                      <Box sx={{ width: '100%', display: 'flex', flexDirection: 'column', height: '100%' }}>
                        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 2 }}>
                          <Box
                            sx={{
                              width: 40,
                              height: 40,
                              borderRadius: 1.5,
                              bgcolor: 'primary.main',
                              bgGradient: 'linear-gradient(135deg, #5E6AD2 0%, #4550A8 100%)',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              color: 'white',
                              boxShadow: '0 4px 12px rgba(94, 106, 210, 0.3)'
                            }}
                          >
                            <DescriptionIcon fontSize="small" />
                          </Box>
                          {doc.category && (
                            <Chip
                              label={doc.category}
                              size="small"
                              sx={{
                                height: 24,
                                fontSize: '0.75rem',
                                bgcolor: 'rgba(255,255,255,0.05)',
                                border: '1px solid',
                                borderColor: 'divider'
                              }}
                            />
                          )}
                        </Box>

                        <Typography variant="subtitle1" component="div" gutterBottom noWrap title={doc.title} sx={{ fontWeight: 600, mb: 1 }}>
                          {doc.title}
                        </Typography>

                        <Box sx={{ mt: 'auto' }}>
                          <Stack spacing={1.5}>
                            <Box sx={{ display: 'flex', alignItems: 'center', color: 'text.secondary' }}>
                              <PersonIcon sx={{ fontSize: 14, mr: 1, opacity: 0.7 }} />
                              <Typography variant="caption" sx={{ fontWeight: 500 }}>
                                {doc.ownerId && userMap[doc.ownerId] ? userMap[doc.ownerId] : (doc.ownerName || '未知')}
                              </Typography>
                            </Box>
                            <Box sx={{ display: 'flex', alignItems: 'center', color: 'text.secondary' }}>
                              <Typography variant="caption" sx={{ opacity: 0.5 }}>
                                更新于 {new Date(doc.updateTime).toLocaleDateString()}
                              </Typography>
                            </Box>
                          </Stack>
                        </Box>
                      </Box>
                    </CardActionArea>
                  </Card>
                </Grid>
              ))
            )}
          </Grid>
        </Box>

        {/* 删除确认对话框 */}
        <Dialog
          open={deleteDialogOpen}
          onClose={() => setDeleteDialogOpen(false)}
        >
          <DialogTitle>确认删除</DialogTitle>
          <DialogContent>
            <DialogContentText>
              您确定要删除文档 "{documentToDelete?.title}" 吗？此操作无法撤销。
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDeleteDialogOpen(false)} color="primary">
              取消
            </Button>
            <Button onClick={handleConfirmDelete} color="error" autoFocus>
              删除
            </Button>
          </DialogActions>
        </Dialog>

      </Container>
    </Box>
  );
};

export default DocumentList;
