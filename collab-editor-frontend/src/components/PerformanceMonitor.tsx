import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  Divider,
  Grid
} from '@mui/material';
import {
  Speed as SpeedIcon,
  Memory as MemoryIcon,
  Code as CodeIcon,
  Web as WebIcon,
  Dns as DnsIcon,
  Language as LanguageIcon,
  Timer as TimerIcon
} from '@mui/icons-material';

interface PerformanceData {
  fps: number;
  memory: {
    usedJSHeapSize: number;
    totalJSHeapSize: number;
    jsHeapSizeLimit: number;
  };
  navigation: {
    domComplete: number;
    domInteractive: number;
    loadEventEnd: number;
  };
  timing: {
    dns: number;
    tcp: number;
    ttfb: number;
    domContentLoaded: number;
    load: number;
  };
}

const PerformanceMonitor: React.FC = () => {
  const [performanceData, setPerformanceData] = useState<PerformanceData>({
    fps: 0,
    memory: { usedJSHeapSize: 0, totalJSHeapSize: 0, jsHeapSizeLimit: 0 },
    navigation: { domComplete: 0, domInteractive: 0, loadEventEnd: 0 },
    timing: { dns: 0, tcp: 0, ttfb: 0, domContentLoaded: 0, load: 0 },
  });

  // 监控 FPS
  useEffect(() => {
    let frameCount = 0;
    let lastTime = performance.now();
    let animationFrameId: number;

    const measureFPS = () => {
      const now = performance.now();
      frameCount++;

      if (now - lastTime >= 1000) {
        setPerformanceData(prev => ({ ...prev, fps: frameCount }));
        frameCount = 0;
        lastTime = now;
      }

      animationFrameId = requestAnimationFrame(measureFPS);
    };

    animationFrameId = requestAnimationFrame(measureFPS);
    return () => cancelAnimationFrame(animationFrameId);
  }, []);

  // 监控内存使用
  useEffect(() => {
    const updateMemory = () => {
      if ((performance as any).memory) {
        setPerformanceData(prev => ({
          ...prev,
          memory: {
            usedJSHeapSize: (performance as any).memory?.usedJSHeapSize || 0,
            totalJSHeapSize: (performance as any).memory?.totalJSHeapSize || 0,
            jsHeapSizeLimit: (performance as any).memory?.jsHeapSizeLimit || 0,
          },
        }));
      }
    };

    updateMemory();
    const interval = setInterval(updateMemory, 5000);
    return () => clearInterval(interval);
  }, []);

  // 获取页面导航和加载时间
  useEffect(() => {
    const updateTiming = () => {
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
      if (navigation) {
        setPerformanceData(prev => ({
          ...prev,
          navigation: {
            domComplete: navigation.domComplete,
            domInteractive: navigation.domInteractive,
            loadEventEnd: navigation.loadEventEnd,
          },
          timing: {
            dns: navigation.domainLookupEnd - navigation.domainLookupStart,
            tcp: navigation.connectEnd - navigation.connectStart,
            ttfb: navigation.responseStart - navigation.requestStart,
            domContentLoaded: navigation.domContentLoadedEventEnd - (navigation as any).navigationStart, // 注意：navigationStart 在 PerformanceNavigationTiming 中可能是 fetchStart
            load: navigation.loadEventEnd - (navigation as any).navigationStart || navigation.duration,
          },
        }));
      }
    };

    // 延迟一点时间获取，确保页面加载完成
    setTimeout(updateTiming, 2000);
  }, []);

  // 格式化字节为可读格式
  const formatBytes = (bytes: number): string => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <Paper
      elevation={4}
      sx={{
        position: 'fixed',
        top: 80, // Avoid overlapping with AppBar
        right: 20,
        width: 280,
        zIndex: 1200,
        p: 2,
        opacity: 0.9,
        transition: 'opacity 0.3s',
        '&:hover': {
          opacity: 1
        },
        display: { xs: 'none', md: 'block' } // Hide on mobile
      }}
    >
      <Typography variant="subtitle2" gutterBottom sx={{ display: 'flex', alignItems: 'center', fontWeight: 'bold' }}>
        <SpeedIcon sx={{ mr: 1, fontSize: 20 }} /> 系统性能监控
      </Typography>
      <Divider sx={{ mb: 1.5 }} />

      <Grid container spacing={1}>
        {/* FPS */}
        <Grid size={6}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Typography variant="caption" color="text.secondary" sx={{ mr: 1 }}>FPS:</Typography>
            <Typography
              variant="body2"
              fontWeight="bold"
              color={performanceData.fps < 30 ? 'error.main' : 'success.main'}
            >
              {performanceData.fps}
            </Typography>
          </Box>
        </Grid>

        {/* Memory */}
        <Grid size={12}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <MemoryIcon sx={{ fontSize: 14, mr: 0.5, color: 'text.secondary' }} />
            <Typography variant="caption" color="text.secondary" sx={{ mr: 1 }}>内存:</Typography>
            <Typography variant="body2" sx={{ fontSize: '0.75rem' }}>
              {formatBytes(performanceData.memory.usedJSHeapSize)} / {formatBytes(performanceData.memory.jsHeapSizeLimit)}
            </Typography>
          </Box>
        </Grid>

        <Grid size={12}><Divider sx={{ my: 0.5 }} /></Grid>

        {/* Network Timing */}
        <Grid size={6}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <DnsIcon sx={{ fontSize: 14, mr: 0.5, color: 'text.secondary' }} />
            <Typography variant="caption" color="text.secondary">DNS:</Typography>
            <Typography variant="body2" sx={{ ml: 'auto', fontSize: '0.75rem' }}>{Math.round(performanceData.timing.dns)}ms</Typography>
          </Box>
        </Grid>

        <Grid size={6}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <LanguageIcon sx={{ fontSize: 14, mr: 0.5, color: 'text.secondary' }} />
            <Typography variant="caption" color="text.secondary">TCP:</Typography>
            <Typography variant="body2" sx={{ ml: 'auto', fontSize: '0.75rem' }}>{Math.round(performanceData.timing.tcp)}ms</Typography>
          </Box>
        </Grid>

        <Grid size={6}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <TimerIcon sx={{ fontSize: 14, mr: 0.5, color: 'text.secondary' }} />
            <Typography variant="caption" color="text.secondary">TTFB:</Typography>
            <Typography variant="body2" sx={{ ml: 'auto', fontSize: '0.75rem' }}>{Math.round(performanceData.timing.ttfb)}ms</Typography>
          </Box>
        </Grid>

        <Grid size={12}><Divider sx={{ my: 0.5 }} /></Grid>

        {/* DOM Timing */}
        <Grid size={6}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <CodeIcon sx={{ fontSize: 14, mr: 0.5, color: 'text.secondary' }} />
            <Typography variant="caption" color="text.secondary">DOM:</Typography>
            <Typography variant="body2" sx={{ ml: 'auto', fontSize: '0.75rem' }}>{Math.round(performanceData.timing.domContentLoaded)}ms</Typography>
          </Box>
        </Grid>

        <Grid size={6}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <WebIcon sx={{ fontSize: 14, mr: 0.5, color: 'text.secondary' }} />
            <Typography variant="caption" color="text.secondary">Load:</Typography>
            <Typography variant="body2" sx={{ ml: 'auto', fontSize: '0.75rem' }}>{Math.round(performanceData.timing.load)}ms</Typography>
          </Box>
        </Grid>
      </Grid>
    </Paper>
  );
};

export default PerformanceMonitor;
