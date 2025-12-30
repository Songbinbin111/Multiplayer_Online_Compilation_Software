import React, { useState, useEffect } from 'react';
import { versionApi, documentApi } from '../api/documentApi';
import { permissionApi } from '../api/permissionApi';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  Paper,
  Stack,
  Tooltip,
  TextField
} from '@mui/material';
import {
  Close as CloseIcon,
  Visibility as VisibilityIcon,
  Restore as RestoreIcon,
  Lock as LockIcon,
  LockOpen as LockOpenIcon,
  CompareArrows as CompareArrowsIcon,
  DeleteOutline as ClearIcon,
  Add as AddIcon
} from '@mui/icons-material';

// 版本信息接口
interface Version {
  id: number;
  docId: number;
  versionName?: string;
  content: string;
  description?: string;
  createdTime: string;
  createdBy: number;
  isLocked?: boolean;
}

// 版本差异结果接口
interface DiffResult {
  version1: Version;
  version2: Version;
  diffRows: string[];
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
  // 版本比较相关状态
  const [selectedVersion1, setSelectedVersion1] = useState<number | ''>('');
  const [selectedVersion2, setSelectedVersion2] = useState<number | ''>('');
  const [diffResult, setDiffResult] = useState<DiffResult | null>(null);
  const [diffLoading, setDiffLoading] = useState(false);
  const [diffError, setDiffError] = useState('');
  const [autoCreateTried, setAutoCreateTried] = useState(false);
  // 新建版本相关状态
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [newVersionName, setNewVersionName] = useState('');
  const [newVersionDesc, setNewVersionDesc] = useState('');
  const [createLoading, setCreateLoading] = useState(false);

  // 获取版本列表
  const fetchVersions = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await versionApi.getVersions(docId);
      const result = response.data;
      if (result?.code === 200) {
        const list = Array.isArray(result.data) ? result.data : [];
        setVersions(list);
      } else {
        setError(result?.message || '获取版本列表失败');
      }
    } catch (err) {
      setError('网络错误，获取版本列表失败');
      console.error('获取版本列表失败:', err);
    } finally {
      setLoading(false);
    }
  };

  // 创建新版本
  const handleCreateVersion = async () => {
    if (!newVersionName.trim()) {
      alert('请输入版本名称');
      return;
    }

    setCreateLoading(true);
    try {
      // 1. 获取当前文档内容
      const contentResp = await documentApi.getContent(docId);
      const contentData = contentResp?.data?.data ?? '';

      // 2. 创建版本
      const createResp = await versionApi.createVersion(
        docId,
        String(contentData || ''),
        newVersionName.trim(),
        newVersionDesc.trim()
      );

      const result = createResp?.data;
      if (result?.code === 200) {
        // alert('创建版本成功');
        setCreateDialogOpen(false);
        setNewVersionName('');
        setNewVersionDesc('');
        fetchVersions();
      } else {
        alert(result?.message || '创建版本失败');
      }
    } catch (err: any) {
      alert(`创建版本失败: ${err.message || '网络错误'}`);
      console.error('创建版本失败:', err);
    } finally {
      setCreateLoading(false);
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
      const result = response.data;
      if (result?.code === 200 && result.data) {
        onVersionSelect(result.data);
      } else {
        alert(result?.message || '获取版本内容失败');
      }
    } catch (err) {
      alert('网络错误，获取版本内容失败');
      console.error('获取版本内容失败:', err);
    }
  };

  // 获取版本差异
  const handleGetDiff = async () => {
    if (!selectedVersion1 || !selectedVersion2) {
      alert('请选择两个版本进行比较');
      return;
    }

    setDiffLoading(true);
    setDiffError('');
    try {
      const response = await versionApi.getVersionDiff(Number(selectedVersion1), Number(selectedVersion2));
      const result = response.data;
      if (result?.code === 200 && result.data) {
        setDiffResult(result.data);
      } else {
        setDiffError(result?.message || '获取版本差异失败');
      }
    } catch (err) {
      setDiffError('网络错误，获取版本差异失败');
      console.error('获取版本差异失败:', err);
    } finally {
      setDiffLoading(false);
    }
  };

  // 清除差异结果
  const handleClearDiff = () => {
    setDiffResult(null);
    setSelectedVersion1('');
    setSelectedVersion2('');
  };

  // 锁定/解锁版本
  const handleLockVersion = async (versionId: number, currentLocked: boolean) => {
    try {
      await versionApi.lockVersion(versionId, !currentLocked);
      fetchVersions(); // 刷新版本列表
    } catch (err: any) {
      alert(`操作失败: ${err.response?.data?.message || '网络错误'}`);
      console.error('版本锁定失败:', err);
    }
  };

  // 组件挂载时获取版本列表
  useEffect(() => {
    fetchVersions();
  }, [docId]);

  // 开发环境下，空列表且具备编辑权限时自动创建初始版本
  useEffect(() => {
    const run = async () => {
      if (import.meta.env.DEV && !loading && versions.length === 0 && !autoCreateTried) {
        setAutoCreateTried(true);
        try {
          const perm = await permissionApi.checkEditPermission(docId);
          const ok = perm?.data?.data === true;
          if (!ok) return;
          const contentResp = await documentApi.getContent(docId);
          const contentData = contentResp?.data?.data ?? '';
          const name = `初始版本`;
          const desc = `由版本面板自动创建`;
          const createResp = await versionApi.createVersion(docId, String(contentData || ''), name, desc);
          if (createResp?.data?.code === 200) {
            fetchVersions();
          }
        } catch { }
      }
    };
    run();
  }, [docId, loading, versions.length, autoCreateTried]);

  return (
    <>
      <Dialog
        open={true}
        onClose={onClose}
        maxWidth="md"
        fullWidth
        PaperProps={{
          sx: { minHeight: '60vh', maxHeight: '80vh' }
        }}
      >
        <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6" component="span">文档版本历史</Typography>
          <Box>
            <Button
              variant="contained"
              size="small"
              startIcon={<AddIcon />}
              onClick={() => setCreateDialogOpen(true)}
              sx={{ mr: 1 }}
            >
              创建新版本
            </Button>
            <IconButton onClick={onClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>

        <DialogContent dividers>
          {(!loading && versions.length === 0) && (
            <Box sx={{ mb: 2 }}>
              <Alert severity="info" sx={{ mb: 1 }}>暂无版本记录</Alert>
              <Button
                variant="contained"
                onClick={async () => {
                  try {
                    const contentResp = await documentApi.getContent(docId);
                    const contentData = contentResp?.data?.data ?? '';
                    const name = `初始版本`;
                    const desc = `由版本面板创建`;
                    const createResp = await versionApi.createVersion(docId, String(contentData || ''), name, desc);
                    const result = createResp?.data;
                    if (result?.code === 200) {
                      fetchVersions();
                    } else {
                      alert(result?.message || '创建版本失败');
                    }
                  } catch (e) {
                    alert('创建版本失败，请稍后重试');
                  }
                }}
                size="small"
              >
                创建首个版本
              </Button>
            </Box>
          )}
          {/* 版本比较区域 */}
          <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: 'background.default' }}>
            <Typography variant="subtitle1" gutterBottom sx={{ display: 'flex', alignItems: 'center' }}>
              <CompareArrowsIcon sx={{ mr: 1 }} /> 版本比较
            </Typography>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="center" sx={{ mb: 2 }}>
              <FormControl size="small" fullWidth>
                <InputLabel>版本 1</InputLabel>
                <Select
                  value={selectedVersion1}
                  label="版本 1"
                  onChange={(e) => setSelectedVersion1(e.target.value as number)}
                >
                  <MenuItem value=""><em>选择版本</em></MenuItem>
                  {versions.map((version) => (
                    <MenuItem key={version.id} value={version.id}>
                      {version.versionName || `版本 ${version.id}`} - {new Date(version.createdTime).toLocaleDateString()}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Typography variant="body2" color="text.secondary">对比</Typography>

              <FormControl size="small" fullWidth>
                <InputLabel>版本 2</InputLabel>
                <Select
                  value={selectedVersion2}
                  label="版本 2"
                  onChange={(e) => setSelectedVersion2(e.target.value as number)}
                >
                  <MenuItem value=""><em>选择版本</em></MenuItem>
                  {versions.map((version) => (
                    <MenuItem key={version.id} value={version.id}>
                      {version.versionName || `版本 ${version.id}`} - {new Date(version.createdTime).toLocaleDateString()}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button
                  variant="contained"
                  onClick={handleGetDiff}
                  disabled={!selectedVersion1 || !selectedVersion2 || selectedVersion1 === selectedVersion2}
                  size="small"
                >
                  比较
                </Button>

                {diffResult && (
                  <Button
                    variant="outlined"
                    color="error"
                    onClick={handleClearDiff}
                    size="small"
                    startIcon={<ClearIcon />}
                  >
                    清除
                  </Button>
                )}
              </Box>
            </Stack>

            {/* 差异结果显示 */}
            {diffResult && (
              <Box sx={{ mt: 2, borderTop: 1, borderColor: 'divider', pt: 2 }}>
                <Typography variant="subtitle2" gutterBottom>
                  差异结果: {diffResult.version1.versionName || `版本 ${diffResult.version1.id}`} → {diffResult.version2.versionName || `版本 ${diffResult.version2.id}`}
                </Typography>

                {diffLoading ? (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
                    <CircularProgress size={24} />
                  </Box>
                ) : diffError ? (
                  <Alert severity="error">{diffError}</Alert>
                ) : (
                  <Paper
                    variant="outlined"
                    sx={{
                      maxHeight: '300px',
                      overflow: 'auto',
                      bgcolor: 'grey.50',
                      p: 1.5,
                      fontFamily: 'monospace',
                      fontSize: '0.875rem'
                    }}
                  >
                    {diffResult.diffRows.map((row, index) => {
                      const isAdd = row.startsWith('+ ');
                      const isRemove = row.startsWith('- ');
                      const isSame = row.startsWith('  ');
                      const text = (isAdd || isRemove || isSame) ? row.slice(2) : row;
                      return (
                        <Box
                          key={index}
                          sx={{
                            mb: 0.5,
                            p: 0.5,
                            borderRadius: 0.5,
                            bgcolor: isAdd ? 'success.light' : isRemove ? 'error.light' : 'transparent',
                            opacity: isAdd || isRemove ? 0.9 : 1
                          }}
                        >
                          <Typography component="span" sx={{ mr: 1 }} color={isAdd ? 'success.main' : isRemove ? 'error.main' : 'text.secondary'}>
                            {isAdd ? '+' : isRemove ? '-' : ' '}
                          </Typography>
                          {text}
                        </Box>
                      );
                    })}
                  </Paper>
                )}
              </Box>
            )}
          </Paper>

          <Typography variant="subtitle1" gutterBottom>历史版本列表</Typography>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
              <CircularProgress />
            </Box>
          ) : error ? (
            <Alert severity="error">{error}</Alert>
          ) : versions.length === 0 ? (
            <Alert severity="info">暂无版本记录</Alert>
          ) : (
            <List>
              {versions.map((version) => (
                <React.Fragment key={version.id}>
                  <ListItem
                    alignItems="flex-start"
                    sx={{
                      bgcolor: version.isLocked ? 'warning.light' : 'inherit',
                      opacity: version.isLocked ? 0.9 : 1,
                      borderRadius: 1,
                      mb: 1,
                      border: 1,
                      borderColor: 'divider'
                    }}
                  >
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="subtitle2">
                            {version.versionName || `版本 ${version.id}`}
                          </Typography>
                          {version.isLocked && <LockIcon fontSize="small" color="action" />}
                        </Box>
                      }
                      secondary={
                        <React.Fragment>
                          <Typography variant="caption" display="block" color="text.secondary">
                            创建时间: {new Date(version.createdTime).toLocaleString()} | 创建人: {version.createdBy}
                          </Typography>
                          {version.description && (
                            <Typography variant="body2" color="text.primary" sx={{ mt: 0.5 }}>
                              {version.description}
                            </Typography>
                          )}
                        </React.Fragment>
                      }
                    />
                    <ListItemSecondaryAction>
                      <Stack direction="row" spacing={1}>
                        <Tooltip title="查看内容">
                          <IconButton
                            edge="end"
                            size="small"
                            onClick={() => handleViewVersion(version.id)}
                            color="primary"
                          >
                            <VisibilityIcon />
                          </IconButton>
                        </Tooltip>

                        <Tooltip title={version.isLocked ? "版本已锁定，无法回滚" : "回滚到此版本"}>
                          <span>
                            <IconButton
                              edge="end"
                              size="small"
                              onClick={() => handleRollback(version.id)}
                              color="warning"
                              disabled={!!version.isLocked}
                            >
                              <RestoreIcon />
                            </IconButton>
                          </span>
                        </Tooltip>

                        <Tooltip title={version.isLocked ? "解锁版本" : "锁定版本"}>
                          <IconButton
                            edge="end"
                            size="small"
                            onClick={() => handleLockVersion(version.id, !!version.isLocked)}
                            color="default"
                          >
                            {version.isLocked ? <LockOpenIcon /> : <LockIcon />}
                          </IconButton>
                        </Tooltip>
                      </Stack>
                    </ListItemSecondaryAction>
                  </ListItem>
                </React.Fragment>
              ))}
            </List>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>关闭</Button>
        </DialogActions>
      </Dialog>

      {/* 创建新版本对话框 */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)}>
        <DialogTitle>创建新版本</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="版本名称"
            fullWidth
            value={newVersionName}
            onChange={(e) => setNewVersionName(e.target.value)}
            placeholder="例如：v1.0 或 完成了第一章"
          />
          <TextField
            margin="dense"
            label="版本描述（可选）"
            fullWidth
            multiline
            rows={3}
            value={newVersionDesc}
            onChange={(e) => setNewVersionDesc(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateDialogOpen(false)}>取消</Button>
          <Button
            onClick={handleCreateVersion}
            variant="contained"
            disabled={createLoading}
          >
            {createLoading ? '创建中...' : '确定创建'}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default VersionControl;
