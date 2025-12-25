import { useState, useRef, useEffect } from 'react';
import { documentApi, versionApi } from '../api/documentApi';

/**
 * 文档管理 Hook
 * 负责封装文档的核心业务逻辑，包括：
 * 1. 获取文档内容
 * 2. 自动/手动保存
 * 3. 版本控制（创建、恢复、预览）
 * 4. 文档导出（PDF、Markdown）
 * 
 * @param docId 文档ID
 * @param checkLogin 登录检查函数
 */
export const useDocument = (docId: string | undefined, checkLogin: () => boolean) => {
  // 文档内容状态
  const [content, setContent] = useState('');
  // 加载状态
  const [loading, setLoading] = useState(true);
  // 上次保存时间
  const [lastSaved, setLastSaved] = useState<string>('');

  // 版本控制相关状态
  const [showVersionControl, setShowVersionControl] = useState<boolean>(false); // 是否显示版本控制面板
  const [showVersionContent, setShowVersionContent] = useState<boolean>(false); // 是否显示版本内容预览
  const [versionContent, setVersionContent] = useState<string>(''); // 预览的版本内容
  const [versionInfo, setVersionInfo] = useState<{ id: number; versionName?: string } | null>(null); // 预览的版本信息

  // 自动保存定时器引用
  const saveTimeoutRef = useRef<number | null>(null);

  /**
   * 从服务器获取最新的文档内容
   */
  const fetchDocumentContent = async () => {
    // 校验登录状态和文档ID
    if (!checkLogin() || !docId) return;

    try {
      setLoading(true);
      const response = await documentApi.getContent(parseInt(docId));

      if (response && response.data && response.data.code === 200) {
        const contentData = response.data.data;
        // 处理不同类型的返回数据
        if (typeof contentData === 'string') {
          setContent(contentData);
          setLastSaved(new Date().toLocaleString());
        } else if (contentData === null || contentData === undefined) {
          setContent('');
        } else {
          console.warn('获取文档内容返回格式异常:', contentData);
          setContent(String(contentData));
        }
      } else {
        console.error('获取文档内容失败，API返回错误:', response);
      }
    } catch (error) {
      console.error('获取文档内容失败:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 将文档内容保存到服务器
   * @param newContent 需要保存的内容
   */
  const saveDocumentContent = async (newContent: string) => {
    if (!docId) return;

    try {
      const response = await documentApi.saveContent(parseInt(docId), newContent);

      if (response && response.data && response.data.code === 200) {
        setLastSaved(new Date().toLocaleString());
      } else {
        console.error('文档保存失败:', response);
      }
    } catch (error) {
      console.error('保存文档内容失败:', error);
    }
  };

  /**
   * 创建新的文档版本
   * 用于手动触发版本备份
   */
  const createVersion = async () => {
    if (!docId) return;

    try {
      const versionName = `V${new Date().getTime()}`;
      const description = `自动版本 ${new Date().toLocaleString()}`;
      await versionApi.createVersion(parseInt(docId), content, versionName, description);
      alert('版本创建成功');
    } catch (error) {
      console.error('创建版本失败:', error);
      alert('创建版本失败，请稍后重试');
    }
  };

  /**
   * 恢复到指定的历史版本
   * @param versionId 目标版本ID
   */
  const restoreVersion = async (versionId: number) => {
    try {
      const response = await documentApi.restoreVersion(versionId);
      if (response.data.code === 200) {
        alert('版本恢复成功');
        // 恢复成功后重新拉取文档内容
        fetchDocumentContent();
      } else {
        alert('恢复失败: ' + response.data.message);
      }
    } catch (error) {
      console.error('恢复版本失败:', error);
      alert('恢复版本失败，请重试');
    }
  };

  /**
   * 导出当前文档为 PDF
   * 发送当前内容到后端生成 PDF 文件并下载
   */
  const exportToPdf = async () => {
    if (!docId) return;

    try {
      const response = await documentApi.exportPdf(parseInt(docId), content);
      // 创建 Blob 对象并生成下载链接
      const url = window.URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `document_${docId}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      // 释放 URL 对象
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('导出PDF失败:', error);
      alert('导出PDF失败，请稍后重试');
    }
  };

  /**
   * 导出当前文档为 Markdown
   * 纯前端实现，直接将内容作为文本文件下载
   */
  const exportToMarkdown = async () => {
    if (!docId) return;

    try {
      // 纯前端导出 Markdown，无需请求后端
      const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `document_${docId}.md`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('导出Markdown失败:', error);
      alert('导出Markdown失败，请稍后重试');
    }
  };

  // 组件挂载时加载文档内容
  useEffect(() => {
    fetchDocumentContent();
    // 清理函数：清除未执行的保存定时器
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
    };
  }, [docId]);

  return {
    content,
    setContent,
    loading,
    lastSaved,
    saveDocumentContent,
    createVersion,
    restoreVersion,
    exportToPdf,
    exportToMarkdown,
    showVersionControl,
    setShowVersionControl,
    showVersionContent,
    setShowVersionContent,
    versionContent,
    setVersionContent,
    versionInfo,
    setVersionInfo,
    saveTimeoutRef
  };
};
