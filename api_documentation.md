# 实时协作编辑系统核心接口文档

## 1. 系统概述

实时协作编辑系统提供了文档的在线创建、编辑、保存和实时协作功能。系统采用前后端分离架构，前端使用React + TypeScript，后端使用Spring Boot + Java，通过RESTful API和WebSocket实现数据交互和实时同步。

## 2. 基础信息

### 2.1 API基础路径
- 后端API基础路径：`http://localhost:8080/api`
- WebSocket基础路径：`ws://localhost:8080/ws/collab`

### 2.2 数据格式
- 请求数据格式：JSON
- 响应数据格式：JSON

### 2.3 响应结构
```json
{
  "code": 200,        // 状态码，200表示成功
  "message": "成功",  // 响应消息
  "data": {}          // 响应数据
}
```

### 2.4 认证方式
- 使用JWT (JSON Web Token) 进行认证
- 请求时在Header中添加 `Authorization: Bearer {token}`

## 3. 用户认证API

### 3.1 用户注册
- **接口地址**：`/register`
- **请求方法**：POST
- **请求参数**：
  | 参数名   | 类型   | 必须 | 说明       |
  |----------|--------|------|------------|
  | username | string | 是   | 用户名     |
  | password | string | 是   | 密码       |
- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "注册成功",
    "data": null
  }
  ```

### 3.2 用户登录
- **接口地址**：`/login`
- **请求方法**：POST
- **请求参数**：
  | 参数名   | 类型   | 必须 | 说明       |
  |----------|--------|------|------------|
  | username | string | 是   | 用户名     |
  | password | string | 是   | 密码       |
- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "userId": 1,
      "username": "testuser"
    }
  }
  ```

## 4. 文档管理API

### 4.1 获取文档列表
- **接口地址**：`/doc/list`
- **请求方法**：GET
- **认证要求**：需要JWT Token
- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": [
      {
        "id": 1,
        "title": "测试文档1",
        "createTime": "2024-12-01T12:00:00",
        "updateTime": "2024-12-01T13:00:00"
      },
      {
        "id": 2,
        "title": "测试文档2",
        "createTime": "2024-12-02T10:00:00",
        "updateTime": "2024-12-02T11:00:00"
      }
    ]
  }
  ```

### 4.2 创建新文档
- **接口地址**：`/doc/create`
- **请求方法**：POST
- **认证要求**：需要JWT Token
- **请求参数**：
  | 参数名 | 类型   | 必须 | 说明     |
  |--------|--------|------|----------|
  | title  | string | 是   | 文档标题 |
- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "创建成功",
    "data": {
      "id": 3,
      "title": "新文档",
      "createTime": "2024-12-03T09:00:00",
      "updateTime": "2024-12-03T09:00:00"
    }
  }
  ```

### 4.3 获取文档内容
- **接口地址**：`/doc/content/{docId}`
- **请求方法**：GET
- **认证要求**：需要JWT Token
- **路径参数**：
  | 参数名 | 类型 | 必须 | 说明   |
  |--------|------|------|--------|
  | docId  | int  | 是   | 文档ID |
- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "获取成功",
    "data": "<p>文档内容</p>"
  }
  ```

### 4.4 保存文档内容
- **接口地址**：`/doc/save`
- **请求方法**：POST
- **认证要求**：需要JWT Token
- **请求参数**：
  | 参数名  | 类型   | 必须 | 说明     |
  |---------|--------|------|----------|
  | docId   | int    | 是   | 文档ID   |
  | content | string | 是   | 文档内容 |
- **响应示例**：
  ```json
  {
    "code": 200,
    "message": "保存成功",
    "data": {
      "updateTime": "2024-12-03T10:00:00"
    }
  }
  ```

## 5. WebSocket API

### 5.1 连接方式
- **连接地址**：`ws://localhost:8080/ws/collab/{docId}`
- **认证要求**：需要JWT Token
- **连接示例**：
  ```javascript
  const wsUrl = `ws://localhost:8080/ws/collab/${docId}`;
  const ws = new WebSocket(wsUrl);
  ```

### 5.2 消息类型

#### 5.2.1 客户端发送消息

##### 5.2.1.1 用户加入
```json
{
  "type": "join",
  "docId": 1,
  "userId": 1,
  "username": "testuser"
}
```

##### 5.2.1.2 内容更新
```json
{
  "type": "content_update",
  "docId": 1,
  "content": "<p>更新后的内容</p>",
  "userId": 1,
  "username": "testuser"
}
```

#### 5.2.2 服务器推送消息

##### 5.2.2.1 用户列表
```json
{
  "type": "user_list",
  "docId": 1,
  "users": [
    {
      "userId": 1,
      "username": "testuser1"
    },
    {
      "userId": 2,
      "username": "testuser2"
    }
  ]
}
```

##### 5.2.2.2 用户加入通知
```json
{
  "type": "user_join",
  "docId": 1,
  "user": {
    "userId": 1,
    "username": "testuser"
  }
}
```

##### 5.2.2.3 用户离开通知
```json
{
  "type": "user_leave",
  "docId": 1,
  "userId": 1,
  "username": "testuser"
}
```

##### 5.2.2.4 内容更新通知
```json
{
  "type": "content_update",
  "docId": 1,
  "content": "<p>更新后的内容</p>",
  "userId": 1,
  "username": "testuser"
}
```

## 6. 错误码说明

| 错误码 | 说明                 |
|--------|----------------------|
| 200    | 成功                 |
| 400    | 请求参数错误         |
| 401    | 未认证或认证失败     |
| 403    | 无权限访问           |
| 404    | 资源不存在           |
| 500    | 服务器内部错误       |
| 4000   | WebSocket连接错误   |

## 7. 接口调用示例

### 7.1 使用axios调用RESTful API
```javascript
import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 添加认证拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 调用API示例
api.get('/doc/list')
  .then(response => {
    console.log('获取文档列表成功:', response.data);
  })
  .catch(error => {
    console.error('获取文档列表失败:', error);
  });
```

### 7.2 使用WebSocket进行实时通信
```javascript
// 建立WebSocket连接
const docId = 1;
const wsUrl = `ws://localhost:8080/ws/collab/${docId}`;
const ws = new WebSocket(wsUrl);

// 连接建立事件
ws.onopen = () => {
  console.log('WebSocket连接已建立');
  // 发送加入消息
  ws.send(JSON.stringify({
    type: 'join',
    docId: docId,
    userId: parseInt(localStorage.getItem('userId')),
    username: localStorage.getItem('username')
  }));
};

// 接收消息事件
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  switch (message.type) {
    case 'user_list':
      console.log('在线用户:', message.users);
      break;
    case 'content_update':
      console.log('内容更新:', message.content);
      break;
    // 处理其他消息类型
  }
};

// 发送内容更新
const sendContentUpdate = (content) => {
  ws.send(JSON.stringify({
    type: 'content_update',
    docId: docId,
    content: content,
    userId: parseInt(localStorage.getItem('userId')),
    username: localStorage.getItem('username')
  }));
};
```

## 8. 安全注意事项

1. **数据加密**：所有敏感数据（如用户密码）在传输和存储时应进行加密
2. **Token安全**：JWT Token应设置合理的过期时间，并使用安全的签名算法
3. **跨域安全**：合理配置CORS策略，只允许受信任的域名访问API
4. **输入验证**：对所有用户输入进行严格验证，防止SQL注入、XSS等攻击
5. **权限控制**：确保用户只能访问和操作自己有权限的资源

## 9. 性能优化建议

1. **使用WebSocket实现实时同步**：减少HTTP请求，提高实时性
2. **内容更新防抖**：避免频繁发送内容更新请求
3. **数据压缩**：对传输的内容进行压缩，减少网络流量
4. **缓存策略**：合理使用缓存减少数据库查询
5. **异步处理**：对耗时操作使用异步处理，提高系统响应速度

## 10. 版本历史

| 版本 | 日期       | 更新内容               |
|------|------------|------------------------|
| 1.0  | 2024-12-13 | 初始版本，完成核心功能 |
