package com.collab.collab_editor_backend.config;

import com.collab.collab_editor_backend.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理所有未捕获的RuntimeException
     * @param e 异常对象
     * @return 统一的错误响应
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        logger.error("发生运行时异常：", e);
        return Result.error(e.getMessage());
    }

    /**
     * 处理所有未捕获的Exception
     * @param e 异常对象
     * @return 统一的错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        logger.error("发生系统异常：", e);
        return Result.error("系统内部错误，请稍后重试");
    }
}
