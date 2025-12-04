package com.collab.collab_editor_backend.util; // 补全包声明（对应util目录）

import lombok.Data; // 导入Lombok的@Data注解

/**
 * 统一响应结果封装类
 * @param <T> 响应数据类型
 */
@Data
public class Result<T> {
    // 状态码：200成功，其他为错误
    private int code;
    // 响应消息
    private String message;
    // 响应数据
    private T data;

    // 私有构造，禁止直接实例化
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 新增：成功响应（带自定义消息，无数据）→ 适配注册接口返回
    public static <T> Result<T> successWithMessage(String message) {
        return new Result<>(200, message, null);
    }

    // 新增：成功响应（带自定义消息+数据）→ 适配登录接口返回Token
    public static <T> Result<T> successWithMessage(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 错误响应
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 错误响应（默认500状态码）
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }
}