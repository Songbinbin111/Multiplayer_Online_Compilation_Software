package com.collab.collab_editor_backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.Map;

/**
 * 错误日志实体类
 */
@Data
public class ErrorLog {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 错误时间戳
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date timestamp;
    
    /**
     * 错误类型
     */
    private String type;
    
    /**
     * 错误消息
     */
    private String message;
    
    /**
     * 错误堆栈信息
     */
    private String stack;
    
    /**
     * 错误发生的URL
     */
    private String url;
    
    /**
     * 错误发生的行号
     */
    private Integer line;
    
    /**
     * 错误发生的列号
     */
    private Integer column;
    
    /**
     * 用户代理信息
     */
    private String userAgent;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 文档ID
     */
    private Long docId;
    
    /**
     * 附加信息
     */
    private String additionalInfo;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
