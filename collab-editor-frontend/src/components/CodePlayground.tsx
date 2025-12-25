import { useState, useRef } from 'react';
import {
  Box,
  Typography,
  Select,
  MenuItem,
  Button,
  AppBar,
  Toolbar,
  IconButton,
  CircularProgress,
  Stack,
  FormControl,
  Chip
} from '@mui/material';
import {
  PlayArrow as PlayIcon,
  Save as SaveIcon,
  ArrowBack as BackIcon,
  Code as CodeIcon,
  Terminal as TerminalIcon
} from '@mui/icons-material';
import Editor, { type OnMount } from '@monaco-editor/react';
import { useNavigate } from 'react-router-dom';

// 模拟支持的语言
const LANGUAGES = [
  { id: 'java', name: 'Java', defaultCode: 'public class Main {\n    public static void main(String[] args) {\n        System.out.println("Hello World!");\n    }\n}' },
  { id: 'python', name: 'Python', defaultCode: 'print("Hello World!")' },
  { id: 'cpp', name: 'C++', defaultCode: '#include <iostream>\n\nint main() {\n    std::cout << "Hello World!" << std::endl;\n    return 0;\n}' },
  { id: 'javascript', name: 'JavaScript', defaultCode: 'console.log("Hello World!");' },
  { id: 'go', name: 'Go', defaultCode: 'package main\n\nimport "fmt"\n\nfunc main() {\n    fmt.Println("Hello World!")\n}' },
];

// 模拟主题
const THEMES = [
  { id: 'vs-dark', name: 'Dark' },
  { id: 'light', name: 'Light' },
];

const CodePlayground: React.FC = () => {
  const navigate = useNavigate();
  const [language, setLanguage] = useState(LANGUAGES[0].id);
  const [editorTheme, setEditorTheme] = useState(THEMES[0].id);
  const [code, setCode] = useState(LANGUAGES[0].defaultCode);
  const [output, setOutput] = useState('');
  const [isRunning, setIsRunning] = useState(false);
  const [executionTime, setExecutionTime] = useState<number | null>(null);
  const editorRef = useRef<any>(null);

  const handleEditorDidMount: OnMount = (editor, _monaco) => {
    editorRef.current = editor;
  };

  const handleLanguageChange = (langId: string) => {
    const targetLang = LANGUAGES.find(l => l.id === langId);
    if (targetLang) {
      setLanguage(langId);
      // 切换语言时，如果当前代码是默认代码，则切换到新语言的默认代码
      // 实际项目中可能需要提示用户是否覆盖
      const currentLang = LANGUAGES.find(l => l.id === language);
      if (code === currentLang?.defaultCode) {
        setCode(targetLang.defaultCode);
      }
    }
  };

  const handleRunCode = async () => {
    setIsRunning(true);
    setOutput('Compiling and executing...');
    setExecutionTime(null);

    const startTime = performance.now();

    // 模拟API调用延迟
    setTimeout(() => {
      const endTime = performance.now();
      setExecutionTime(Math.round(endTime - startTime));

      // 模拟输出结果
      // 实际项目中这里应该调用后端 compilation API
      let mockOutput = '';
      switch (language) {
        case 'java':
          mockOutput = 'Hello World!\n\nProcess finished with exit code 0';
          break;
        case 'python':
          mockOutput = 'Hello World!\n';
          break;
        case 'cpp':
          mockOutput = 'Hello World!\n\nProgram returned: 0';
          break;
        default:
          mockOutput = `Output for ${language}:\nHello World!`;
      }

      setOutput(mockOutput);
      setIsRunning(false);
    }, 1500);
  };

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column', bgcolor: 'background.default' }}>
      {/* 顶部导航栏 */}
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar variant="dense">
          <IconButton edge="start" color="inherit" onClick={() => navigate(-1)} sx={{ mr: 2 }}>
            <BackIcon />
          </IconButton>
          <CodeIcon sx={{ mr: 1, color: 'primary.main' }} />
          <Typography variant="h6" color="inherit" component="div" sx={{ flexGrow: 1 }}>
            Online Compiler
          </Typography>

          <Stack direction="row" spacing={2} alignItems="center">
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <Select
                value={language}
                onChange={(e) => handleLanguageChange(e.target.value)}
                displayEmpty
                inputProps={{ 'aria-label': 'Select Language' }}
              >
                {LANGUAGES.map((lang) => (
                  <MenuItem key={lang.id} value={lang.id}>{lang.name}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl size="small" sx={{ minWidth: 100 }}>
              <Select
                value={editorTheme}
                onChange={(e) => setEditorTheme(e.target.value)}
              >
                {THEMES.map((t) => (
                  <MenuItem key={t.id} value={t.id}>{t.name}</MenuItem>
                ))}
              </Select>
            </FormControl>

            <Button
              variant="contained"
              color="success"
              startIcon={isRunning ? <CircularProgress size={20} color="inherit" /> : <PlayIcon />}
              onClick={handleRunCode}
              disabled={isRunning}
            >
              Run
            </Button>

            <Button
              variant="outlined"
              startIcon={<SaveIcon />}
            >
              Save
            </Button>
          </Stack>
        </Toolbar>
      </AppBar>

      {/* 主体内容区 */}
      <Box sx={{ flexGrow: 1, overflow: 'hidden', display: 'flex', flexDirection: { xs: 'column', md: 'row' } }}>
        {/* 左侧代码编辑器 */}
        <Box sx={{ flex: { xs: '1 0 auto', md: '0 0 65%' }, height: '100%', borderRight: { md: 1 }, borderColor: 'divider' }}>
          <Editor
            height="100%"
            language={language === 'c++' ? 'cpp' : language}
            theme={editorTheme}
            value={code}
            onChange={(value) => setCode(value || '')}
            onMount={handleEditorDidMount}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              scrollBeyondLastLine: false,
              automaticLayout: true,
              tabSize: 4,
            }}
          />
        </Box>

        {/* 右侧输出面板 */}
        <Box sx={{ flex: { xs: '1 0 auto', md: '0 0 35%' }, height: '100%', display: 'flex', flexDirection: 'column' }}>
          <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', bgcolor: 'background.paper' }}>
            <Stack direction="row" alignItems="center" spacing={1}>
              <TerminalIcon color="action" />
              <Typography variant="subtitle1" fontWeight="bold">
                Console Output
              </Typography>
            </Stack>
          </Box>

          <Box sx={{
            flexGrow: 1,
            p: 2,
            bgcolor: '#1e1e1e',
            color: '#f0f0f0',
            fontFamily: 'Monospace',
            overflow: 'auto'
          }}>
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
              {output || 'Click \"Run\" to see output...'}
            </pre>
          </Box>

          {executionTime !== null && (
            <Box sx={{ p: 1, bgcolor: 'background.paper', borderTop: 1, borderColor: 'divider' }}>
              <Stack direction="row" justifyContent="space-between" alignItems="center">
                <Typography variant="caption" color="text.secondary">
                  Execution Time: {executionTime}ms
                </Typography>
                <Chip label="Success" color="success" size="small" variant="outlined" />
              </Stack>
            </Box>
          )}
        </Box>
      </Box>
    </Box>
  );
};

export default CodePlayground;
