-- 创建用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) DEFAULT 'viewer', -- 角色字段：admin, editor, viewer
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    reset_token VARCHAR(255),
    reset_token_expiry TIMESTAMP
);

-- 创建文档表
CREATE TABLE IF NOT EXISTS t_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    minio_key VARCHAR(255),
    category VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    owner_id BIGINT NOT NULL,
    tags VARCHAR(255)
);

-- 创建文档权限表
CREATE TABLE IF NOT EXISTS t_doc_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    permission_type INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_user (doc_id, user_id)
);

-- 创建文档版本表
CREATE TABLE IF NOT EXISTS t_document_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    content TEXT,
    minio_key VARCHAR(255),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_user_id BIGINT NOT NULL,
    description VARCHAR(255)
);

-- 创建在线用户表
CREATE TABLE IF NOT EXISTS t_online_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_user (doc_id, user_id)
);

-- 创建聊天消息表
CREATE TABLE IF NOT EXISTS t_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    send_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read INT DEFAULT 0
);

-- 创建任务表
CREATE TABLE IF NOT EXISTS t_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    assignee_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    status INT DEFAULT 0,
    deadline TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建用户满意度调查表
CREATE TABLE IF NOT EXISTS t_survey (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    score INT NOT NULL,
    comment TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建通知表
CREATE TABLE IF NOT EXISTS t_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50),
    content TEXT NOT NULL,
    doc_id BIGINT,
    related_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建评论表
CREATE TABLE IF NOT EXISTS t_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_id BIGINT,
    start_pos INT,
    end_pos INT,
    selected_text VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建操作日志表
CREATE TABLE IF NOT EXISTS t_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT,
    content TEXT,
    ip_address VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建错误日志表
CREATE TABLE IF NOT EXISTS error_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp DATETIME NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    stack TEXT,
    url VARCHAR(255),
    line INT,
    column INT,
    user_agent TEXT,
    user_id BIGINT,
    doc_id BIGINT,
    additional_info TEXT,
    create_time DATETIME NOT NULL
);

-- 创建用户行为表
CREATE TABLE IF NOT EXISTS t_user_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    object_id BIGINT,
    object_type VARCHAR(50),
    details TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_error_log_timestamp ON error_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_error_log_create_time ON error_log(create_time);
CREATE INDEX IF NOT EXISTS idx_error_log_user_id ON error_log(user_id);
CREATE INDEX IF NOT EXISTS idx_error_log_doc_id ON error_log(doc_id);
CREATE INDEX IF NOT EXISTS idx_error_log_type ON error_log(type);
CREATE INDEX IF NOT EXISTS idx_user_activity_user_id ON t_user_activity(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_activity_type ON t_user_activity(activity_type);
CREATE INDEX IF NOT EXISTS idx_user_activity_created_at ON t_user_activity(created_at);

-- 创建视频会议表
CREATE TABLE IF NOT EXISTS t_video_meeting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id VARCHAR(50) NOT NULL,
    channel_name VARCHAR(50) NOT NULL,
    token VARCHAR(255),
    creator_id BIGINT NOT NULL,
    doc_id BIGINT NOT NULL,
    title VARCHAR(255),
    status INT DEFAULT 0,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
