import { useEffect, useRef, forwardRef, useImperativeHandle } from 'react';
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';

// 创建一个ReactQuillWrapper组件，并使用忽略特定警告的方式处理findDOMNode问题
export const ReactQuillWrapper = forwardRef((props: any, ref: any) => {
  const innerRef = useRef<ReactQuill>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // 在组件挂载时暂时忽略findDOMNode警告
  useEffect(() => {
    // 保存原始的console.warn
    const originalWarn = console.warn;
    const originalError = console.error;

    // 重写console.warn，过滤掉ReactQuill的findDOMNode警告
    console.warn = (...args) => {
      if (args.length > 0 && typeof args[0] === 'string') {
        const message = args[0];
        // 检查是否是来自ReactQuill的findDOMNode警告
        if (message.includes('findDOMNode is deprecated')) {
          return; // 忽略此警告
        }
      }
      // 其他警告正常输出
      originalWarn.apply(console, args);
    };

    // 重写console.error，过滤掉ReactQuill的findDOMNode警告
    console.error = (...args) => {
      if (args.length > 0 && typeof args[0] === 'string') {
        const message = args[0];
        if (message.includes('findDOMNode is deprecated')) {
          return;
        }
      }
      originalError.apply(console, args);
    };

    // 组件卸载时恢复原始的console.warn
    return () => {
      console.warn = originalWarn;
      console.error = originalError;
    };
  }, []);

  useImperativeHandle(ref, () => innerRef.current!);

  return (
    <div ref={containerRef} className="quill-container" style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <ReactQuill ref={innerRef} {...props} style={{ height: '100%', display: 'flex', flexDirection: 'column', flex: 1 }} />
      <style>{`
        .quill-container .ql-container {
          flex: 1;
          overflow-y: auto;
          font-family: inherit;
          font-size: 1rem;
        }
        .quill-container .ql-editor {
          position: relative;
          min-height: 100%;
        }
        .quill-container .ql-toolbar {
          border-top: none;
          border-left: none;
          border-right: none;
          border-bottom: 1px solid rgba(0, 0, 0, 0.12);
        }
      `}</style>
    </div>
  );
});

ReactQuillWrapper.displayName = 'ReactQuillWrapper';
