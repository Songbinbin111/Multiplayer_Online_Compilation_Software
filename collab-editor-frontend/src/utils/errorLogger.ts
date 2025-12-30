// 错误日志记录服务

interface ErrorLog {
  timestamp: number;
  type: string;
  message: string;
  stack?: string;
  url?: string;
  line?: number;
  column?: number;
  userAgent: string;
  userId?: string;
  docId?: string;
  additionalInfo?: Record<string, any>;
}

class ErrorLogger {
  private logs: ErrorLog[] = [];
  private batchSize = 10;
  private flushInterval = 5000;
  private userId?: string;
  private docId?: string;
  private flushTimer: number | null = null;

  constructor() {
    this.init();
  }

  private init() {
    // 捕获全局错误
    window.addEventListener('error', this.handleGlobalError.bind(this));

    // 捕获未处理的Promise拒绝
    window.addEventListener('unhandledrejection', this.handleUnhandledRejection.bind(this));

    // 设置定期刷新定时器
    this.flushTimer = setInterval(() => {
      this.flushLogs();
    }, this.flushInterval);

    // 页面卸载时刷新日志
    window.addEventListener('beforeunload', () => {
      this.flushLogs();
    });
  }

  private handleGlobalError(event: ErrorEvent) {
    const log: ErrorLog = {
      timestamp: Date.now(),
      type: 'GlobalError',
      message: event.message,
      stack: event.error?.stack,
      url: event.filename,
      line: event.lineno,
      column: event.colno,
      userAgent: navigator.userAgent,
      userId: this.userId,
      docId: this.docId,
    };

    this.addLog(log);
  }

  private handleUnhandledRejection(event: PromiseRejectionEvent) {
    const log: ErrorLog = {
      timestamp: Date.now(),
      type: 'UnhandledRejection',
      message: event.reason?.message || String(event.reason),
      stack: event.reason?.stack,
      userAgent: navigator.userAgent,
      userId: this.userId,
      docId: this.docId,
    };

    this.addLog(log);
  }

  public setUserId(userId: string) {
    this.userId = userId;
  }

  public setDocId(docId: string) {
    this.docId = docId;
  }

  public logError(error: Error | string, type: string = 'CustomError', additionalInfo?: Record<string, any>) {
    const log: ErrorLog = {
      timestamp: Date.now(),
      type,
      message: typeof error === 'string' ? error : error.message,
      stack: typeof error === 'string' ? undefined : error.stack,
      userAgent: navigator.userAgent,
      userId: this.userId,
      docId: this.docId,
      additionalInfo,
    };

    this.addLog(log);
  }

  private addLog(log: ErrorLog) {
    this.logs.push(log);

    // 如果日志数量达到批量大小，立即刷新
    if (this.logs.length >= this.batchSize) {
      this.flushLogs();
    }
  }

  private async flushLogs() {
    if (this.logs.length === 0) return;
    if (import.meta.env.DEV) {
      this.logs = [];
      return;
    }

    const logsToSend = [...this.logs].map(log => ({
      ...log,
      timestamp: new Date(log.timestamp),
      userId: log.userId ? parseInt(log.userId) : undefined,
      docId: log.docId ? parseInt(log.docId) : undefined,
      additionalInfo: log.additionalInfo ? JSON.stringify(log.additionalInfo) : undefined
    }));
    this.logs = [];

    try {
      // 发送日志到后端
      await fetch('/api/error-logs/batch', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(logsToSend),
      });
    } catch (error) {
      // 如果发送失败，将日志保存到localStorage
      console.error('Failed to send error logs:', error);
      this.saveToLocalStorage(logsToSend);
    }
  }

  private saveToLocalStorage(logs: any[]) {
    try {
      const existingLogs = JSON.parse(localStorage.getItem('unsentErrorLogs') || '[]');
      const updatedLogs = [...existingLogs, ...logs];
      localStorage.setItem('unsentErrorLogs', JSON.stringify(updatedLogs));
    } catch (error) {
      console.error('Failed to save error logs to localStorage:', error);
    }
  }

  public async sendLocalStorageLogs() {
    try {
      if (import.meta.env.DEV) return;
      const unsentLogs = JSON.parse(localStorage.getItem('unsentErrorLogs') || '[]');
      if (unsentLogs.length > 0) {
        const formattedLogs = unsentLogs.map((log: any) => ({
          ...log,
          timestamp: new Date(log.timestamp),
          userId: log.userId ? parseInt(log.userId) : undefined,
          docId: log.docId ? parseInt(log.docId) : undefined,
          additionalInfo: log.additionalInfo ? JSON.stringify(log.additionalInfo) : undefined
        }));
        await fetch('/api/error-logs/batch', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(formattedLogs),
        });
        localStorage.removeItem('unsentErrorLogs');
      }
    } catch (error) {
      console.error('Failed to send localStorage error logs:', error);
    }
  }

  public destroy() {
    if (this.flushTimer) {
      clearInterval(this.flushTimer);
    }
    window.removeEventListener('error', this.handleGlobalError);
    window.removeEventListener('unhandledrejection', this.handleUnhandledRejection);
    this.flushLogs();
  }
}

// 创建单例实例
export const errorLogger = new ErrorLogger();
export default ErrorLogger;
