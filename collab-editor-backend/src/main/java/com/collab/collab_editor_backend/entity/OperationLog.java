package com.collab.collab_editor_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@TableName(value = "t_operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId; // 操作用户ID
    private String username; // 操作用户名
    private String operationType; // 操作类型（login, register, update_profile, update_role等）
    private String operationContent; // 操作内容
    private String ipAddress; // 操作IP地址
    private String userAgent; // 浏览器信息
    private Boolean success; // 操作是否成功
    private String errorMessage; // 错误信息（如果操作失败）
    private LocalDateTime createTime; // 操作时间
}
