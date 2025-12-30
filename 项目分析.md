# 项目分析

## 项目概览
- 名称：多人协作在线编辑系统（collab-editor）
- 功能：文档协作编辑、权限分配、评论聊天、实时协作、用户行为与满意度监控、视频会议
- 架构：前后端分离，前端 Vite + React + MUI，后端 Spring Boot + MyBatis-Plus，数据库 MySQL，存储 MinIO，鉴权 JWT

## 技术栈
- 前端：React 18、TypeScript、Vite、Material-UI、Axios
- 后端：Spring Boot 3、MyBatis-Plus、JWT、HikariCP、WebSocket
- 数据库：MySQL；对象存储：MinIO；缓存：Redis（预留）

## 运行与环境
- 前端环境变量：[.env](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/.env)  
  - VITE_API_BASE_URL=http://localhost:8080
- 后端端口与配置：[application.properties](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/resources/application.properties#L33-L55)  
  - server.port=8080
- 本地启动
  - 后端：`mvn spring-boot:run`
  - 前端：`npm run dev -- --port 5173`
- 登录入口与自动登录
  - 路由：见 [App.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/App.tsx#L332-L344)
  - 开发自动登录开关：见 [useAuth.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/hooks/useAuth.ts#L16-L36)  
    - 通过 `?noAutoLogin=1` 可强制进入登录页

## 核心模块
- 文档编辑器：[Editor.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/Editor.tsx)  
  - 富文本、协作钩子、评论、任务、权限面板集成
- 文档列表与分类：[DocumentList.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/DocumentList.tsx)  
  - 列表、搜索、通知、退出登录
- 权限分配面板：[PermissionPanel.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/PermissionPanel.tsx)  
  - 支持查看/编辑两种权限，连续分配优化，成功提示
- 监控与管理：  
  - 管理台入口：[AdminPanel.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/AdminPanel.tsx)  
  - 行为分析：[UserBehavior.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/admin/UserBehavior.tsx)  
  - 满意度统计：[SurveyStats.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/admin/SurveyStats.tsx)

## 鉴权与权限
- 登录接口：后端 [UserController.login](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/UserController.java#L169-L249)
- JWT 拦截器与白名单：[WebConfig](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/config/WebConfig.java#L63-L83)  
  - 白名单：/api/login, /api/register, /api/monitor/**, /actuator/**
- 文档权限类型：0=查看、1=编辑  
  - 前端下拉仅保留这两种；列表中只展示可编辑范围
- 权限校验：服务层与 WebSocket 在打开文档和订阅时进行校验

## 数据模型与表
- 用户表、文档表、文档权限表、文档版本表、错误日志表、用户行为表、视频会议表  
  - 初始化与补充字段：见 [schema.sql](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/resources/schema.sql) 与 [DatabaseInitializer.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/config/DatabaseInitializer.java)

## API 约定
- 统一返回结构：code、message、data
- 典型接口
  - 文档列表：GET /api/doc/list
  - 文档分类：GET /api/doc/categories
  - 权限：/api/permission/assign、/api/permission/update、/api/permission/remove
  - 用户：/api/user/list、/api/user/profile
  - 登录：POST /api/login

## 路由与页面
- 受保护路由：根路径、编辑器、个人资料、操场、监控、管理页  
  - 逻辑：见 [App.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/App.tsx#L332-L344)
- 登录页与注册/重置密码：见 [Login.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/Login.tsx)

## 需求详细分析：1.1 用户注册与登录

### 1. 相关文件
**后端**
- 控制器：[UserController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/UserController.java)
  - `register`: 注册接口
  - `login`: 登录接口
  - `requestPasswordReset`: 请求密码重置
  - `resetPassword`: 执行密码重置
- 服务层：[UserServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/UserServiceImpl.java)
- 数据访问层：[UserMapper.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/mapper/UserMapper.java)
- 实体类：`User.java`, `UserLoginDTO.java`, `PasswordResetRequestDTO.java`, `PasswordResetDTO.java`
- 工具类：`JwtUtil.java` (Token生成), `PasswordEncoder` (密码加密)

**前端**
- 页面组件：
  - [Login.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/Login.tsx) (登录)
  - [PasswordReset.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/PasswordReset.tsx) (找回密码)
  - `Register.tsx` (注册页面)
- API封装：[userApi.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/api/userApi.ts)

### 2. 设计 (Design)

#### 注册设计
- **输入支持**：用户名、邮箱、手机号（三选一作为唯一标识），密码。
- **校验逻辑**：
  - 必填校验：密码不能为空。
  - 唯一性校验：邮箱和手机号必须在数据库中唯一。
- **安全策略**：
  - 密码存储前使用 `BCrypt` 算法进行加密，数据库中不存储明文密码。
  - 默认角色设置为 `editor`（编辑者）。
- **业务流程**：校验参数 -> 查重 -> 加密密码 -> 插入数据库 -> 记录活动日志。

#### 登录设计
- **认证方式**：用户名 + 密码。
- **状态管理**：无状态 JWT (JSON Web Token)。
- **流程**：
  1. 用户提交凭证。
  2. 后端查询用户是否存在。
  3. `passwordEncoder.matches()` 比对密文密码。
  4. 验证通过后生成 JWT，包含 `userId` 和 `username`。
  5. 返回 Token 及用户信息，前端存储于 `localStorage`。

#### 密码找回与重置设计
- **两阶段流程**：
  1. **请求重置** (`/api/user/reset-password/request`)：用户输入邮箱或手机号 -> 后端验证存在性 -> 生成重置令牌 (Reset Token) 并设置过期时间 -> (模拟)发送验证码/链接或直接返回 Token 用于演示。
  2. **执行重置** (`/api/user/reset-password`)：用户提交新密码和令牌 -> 后端校验令牌有效性 -> 更新密码 -> 清除令牌。

### 3. 实现 (Implementation)

#### 后端核心代码逻辑
```java
// UserController.java
@PostMapping("/api/register")
public Result register(@RequestBody User user) {
    // 1. 校验：至少提供用户名/邮箱/手机号之一
    if (StringUtils.isAllBlank(user.getUsername(), user.getEmail(), user.getPhone())) {
        return Result.error("至少需要提供用户名、邮箱或手机号中的一个");
    }
    // 2. 查重：检查邮箱/手机号是否已存在
    if (userMapper.selectByEmail(user.getEmail()) != null) return Result.error("邮箱已被占用");
    // 3. 加密：BCrypt
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    // 4. 入库
    userMapper.insert(user);
    return Result.successWithMessage("注册成功");
}

@PostMapping("/api/login")
public Result login(@RequestBody User user) {
    // 1. 查询用户
    User dbUser = userMapper.selectByUsername(user.getUsername());
    // 2. 比对密码
    if (dbUser == null || !passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
        return Result.error("用户名或密码错误");
    }
    // 3. 生成Token
    String token = jwtUtil.generateToken(dbUser.getId(), dbUser.getUsername());
    return Result.success(Map.of("token", token, "user", dbUser));
}
```

#### 前端交互逻辑
- **Axios 请求**：使用 `userApi.register` 和 `userApi.login` 发起请求。
- **Token 处理**：登录成功后，将 `token` 写入 `localStorage`。后续请求通过拦截器自动携带 `Authorization: Bearer <token>` 头（需完善拦截器配置）。
- **重置密码页面**：根据 URL 参数 `?token=...` 判断是“请求阶段”还是“重置阶段”。

### 4. 测试 (Testing)

#### 单元测试场景
1. **注册测试**：
   - 输入：用户名+密码 -> 预期：成功，DB新增记录，密码为密文。
   - 输入：已存在的邮箱 -> 预期：失败，提示“已被占用”。
2. **登录测试**：
   - 输入：正确用户名+错误密码 -> 预期：失败。
   - 输入：正确用户名+正确密码 -> 预期：成功，返回 Token 且不为空。
3. **重置密码测试**：
   - 请求重置 -> 获取 Token -> 使用 Token 重置密码 -> 使用新密码登录 -> 预期：成功。

#### 手动验证步骤
1. 打开注册页 `/register`，使用新手机号注册。
2. 检查数据库 `t_user` 表，确认记录生成且密码已加密。
3. 打开登录页 `/login`，使用刚注册的账号登录，检查浏览器控制台 Application -> Local Storage 是否有 `token`。
4. 点击“忘记密码”，输入手机号，模拟获取 Token（查看网络响应）。
5. 跳转重置页，输入新密码，提交。
6. 退出登录，使用新密码重新登录。

## 1.2 用户信息管理

### 1. 功能模块
- **个人资料编辑**：修改昵称、邮箱、手机号等基本信息。
- **头像上传与设置**：支持用户上传自定义头像，后端对接 MinIO 存储。
- **联系信息管理**：邮箱与手机号的绑定与唯一性校验。

### 2. 相关文件
**后端**
- 控制器：[UserController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/UserController.java)
  - `updateUser`: 更新用户信息接口
  - `uploadAvatar`: 头像上传接口
  - `getUserProfile`: 获取个人资料接口
- 服务层：[UserServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/UserServiceImpl.java)
- 数据传输对象：[UserProfileDTO.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/dto/UserProfileDTO.java)
- 工具类：`MinIOUtil.java` (文件存储)

**前端**
- 页面组件：[Profile.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/Profile.tsx)
- API封装：[userApi.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/api/userApi.ts)

### 3. 核心流程

#### 个人资料更新
1. **前端**：用户在 `Profile` 页面修改表单 -> 点击保存 -> 调用 `userApi.updateProfile`。
2. **后端**：
   - `UserController` 接收 `UserProfileDTO`。
   - `UserService` 校验用户是否存在。
   - **唯一性校验**：检查新提交的邮箱/手机号是否已被其他用户占用。
   - 执行更新并刷新 `updateTime`。

#### 头像上传
1. **前端**：用户点击头像 -> 选择文件 -> `handleAvatarChange` 触发 `userApi.uploadAvatar`。
2. **后端**：
   - 接收 `MultipartFile`。
   - `MinIOUtil` 上传文件至对象存储。
   - 返回文件访问 URL。
3. **前端**：获取 URL 后更新 `editForm.avatarUrl`，随资料更新请求一并保存。

### 4. 关键代码逻辑

**后端：唯一性校验 (UserServiceImpl.java)**
```java
// 验证邮箱唯一性（排除自身）
if (dto.getEmail() != null && !dto.getEmail().equals(existingUser.getEmail())) {
    User userByEmail = userMapper.selectByEmail(dto.getEmail());
    if (userByEmail != null && !userByEmail.getId().equals(userId)) {
        return Result.error("该邮箱已被其他用户使用");
    }
}
```

**前端：头像上传与保存 (Profile.tsx)**
```typescript
const handleSaveProfile = async () => {
  // 1. 先上传头像
  if (avatarFile) {
    const avatarResponse = await userApi.uploadAvatar(avatarFile);
    if (avatarResponse.code === 200) {
      editForm.avatarUrl = avatarResponse.data;
    }
  }
  // 2. 更新其余信息
  const response = await userApi.updateProfile(editForm);
  // ...
};
```

## 1.3 用户权限管理

### 1. 角色定义
系统采用 **RBAC (基于角色的访问控制)** 与 **ACL (访问控制列表)** 相结合的混合模式。

- **系统级角色** (User Role):
  - `admin`: 系统管理员，拥有所有文档和系统设置的完全访问权限，可查看监控看板。
  - `editor` / `user`: 普通用户，默认角色，仅能访问自己创建或被授权的文档。

- **文档级角色** (Document Permission):
  - **所有者 (Owner, Type=2)**: 文档创建者，拥有最高权限（删除、分配权限）。
  - **编辑者 (Editor, Type=1)**: 可编辑文档内容、查看协作人员。
  - **查看者 (Viewer, Type=0)**: 仅只读访问，无法修改内容。

### 2. 权限分配
权限分配发生在文档维度，由文档所有者或管理员操作。

- **UI 实现**：[PermissionPanel.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/PermissionPanel.tsx) 提供用户搜索与权限下拉选择（查看/编辑）。
- **API 接口**：[permissionApi.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/api/permissionApi.ts)
  - `POST /api/permission/assign/{docId}/{userId}/{type}`: 分配权限
  - `PUT /api/permission/update/...`: 更新权限
  - `DELETE /api/permission/remove/...`: 移除权限

### 3. 访问控制列表 (ACL)
后端通过 `t_doc_permission` 表维护 ACL，服务层在业务操作前进行校验。

**核心校验逻辑 ([DocPermissionServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/DocPermissionServiceImpl.java))**:
```java
public boolean hasViewPermission(Long docId, Long userId) {
    // 1. 检查文档是否存在
    // 2. 检查是否为所有者 (OwnerId == userId)
    // 3. 查询 ACL 表 (t_doc_permission) 是否有记录
}
```

### 4. 操作日志记录
系统记录关键操作以供审计和行为分析。

- **实现机制**：
  - 服务层：[OperationLogService.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/OperationLogService.java) 提供 `recordLog` 方法（异步执行）。
  - 埋点位置：登录/注册、文档查看/更新、权限变更等业务方法中。
- **日志字段**：
  - `userId`, `username`: 操作人
  - `operationType`: 操作类型 (login, view_document, update_document)
  - `ipAddress`, `userAgent`: 环境信息
  - `success`, `errorMessage`: 执行结果
- **展示**：[MonitorDashboard.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/pages/MonitorDashboard.tsx) 从 `/api/operation-logs` 获取并展示日志表格。

## 2.1 文档创建与编辑

### 1. 相关文件
- **前端**：
  - [Editor.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/Editor.tsx): 编辑器主容器。
  - [ReactQuillWrapper.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/ReactQuillWrapper.tsx): 封装 React-Quill 组件。
  - [useDocument.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/hooks/useDocument.ts): 处理加载、保存、导出逻辑。
- **后端**：
  - [DocumentController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/DocumentController.java)
  - [DocumentServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/DocumentServiceImpl.java)

### 2. 设计与实现

**功能设计**：
- **富文本编辑**：基于 `React-Quill`，支持基础格式与图片上传。
- **自动保存**：前端采用防抖机制（Debounce），用户停止输入 1 秒后自动调用保存接口；已修复 WebSocket 消息回环导致的自动输入问题。
- **模板库**：创建文档时可选预置模板内容。

**核心代码 (useDocument.ts - 自动保存)**：
```typescript
// 防抖保存逻辑
useEffect(() => {
  if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
  if (content !== lastSavedContent) {
    saveTimeoutRef.current = window.setTimeout(() => {
      saveDocumentContent(content);
    }, 2000); // 2秒无操作自动保存
  }
}, [content]);
```

**核心代码 (DocumentServiceImpl.java - 保存内容)**：
```java
@Override
public Result<?> saveContent(Long docId, String content, Long userId) {
    // 1. 校验权限
    if (!permissionService.hasEditPermission(docId, userId)) {
        return Result.error(403, "无权编辑");
    }
    // 2. 更新主表内容
    document.setContent(content);
    documentMapper.updateById(document);
    // 3. 异步触发版本创建
    versionService.createVersion(docId, content, "自动保存", null, userId);
    return Result.success();
}
```

### 3. 测试验证
- **测试用例 1 (编辑)**：输入文本、插入图片，刷新页面确认内容保留。
- **测试用例 2 (自动保存)**：输入内容后停止操作，观察网络请求面板是否有 `/api/doc/save` 请求。
- **测试用例 3 (Markdown 导出)**：点击导出按钮，检查下载的 `.md` 文件内容格式是否正确。

## 2.2 文档版本控制

### 1. 相关文件
- **前端**：
  - [VersionControl.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/VersionControl.tsx)
- **后端**：
  - [DocumentVersionController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/DocumentVersionController.java)
  - [DocumentVersionServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/DocumentVersionServiceImpl.java)

### 2. 设计与实现

**功能设计**：
- **快照存储**：每次保存（自动/手动）生成全量快照存储于 `t_doc_version`。
- **版本回滚**：将指定历史版本的内容覆盖当前文档，并为回滚操作本身生成一条新记录（防止数据丢失）。
- **版本锁定**：管理员可锁定特定版本，防止被自动清理（清理逻辑待实现）。

**核心代码 (DocumentVersionServiceImpl.java - 回滚)**：
```java
@Override
public Result<?> rollbackToVersion(Long docId, Long versionId, Long userId) {
    // 1. 获取目标版本内容
    DocumentVersion targetVersion = versionMapper.selectById(versionId);
    
    // 2. 备份当前状态（作为"回滚前版本"）
    Document currentDoc = docMapper.selectById(docId);
    createVersion(docId, currentDoc.getContent(), "回滚前备份", null, userId);
    
    // 3. 覆盖当前文档内容
    currentDoc.setContent(targetVersion.getContent());
    docMapper.updateById(currentDoc);
    return Result.success();
}
```

### 3. 测试验证
- **测试用例 1 (版本生成)**：多次编辑并保存，查看版本列表是否增加。
- **测试用例 2 (回滚)**：选择旧版本点击“恢复”，确认编辑器内容变更，且版本列表新增一条“回滚前备份”记录。
- **测试用例 3 (权限)**：尝试使用无权限用户调用回滚接口，预期返回 403。

## 2.3 文档分类与搜索

### 1. 相关文件
- **前端**：[DocumentList.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/DocumentList.tsx)
- **后端**：[DocumentServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/DocumentServiceImpl.java)

### 2. 设计与实现

**功能设计**：
- **多维度搜索**：支持关键词（标题/内容）、标签、作者、时间范围组合查询。
- **权限过滤**：搜索结果仅包含用户有权访问（Owner/Editor/Viewer）的文档。

**核心代码 (DocumentServiceImpl.java - 动态查询)**：
```java
public Result<?> search(Long userId, String keyword, String tags, ...) {
    LambdaQueryWrapper<Document> query = new LambdaQueryWrapper<>();
    
    // 1. 关键词模糊匹配
    if (StringUtils.hasText(keyword)) {
        query.and(q -> q.like(Document::getTitle, keyword)
                        .or().like(Document::getContent, keyword));
    }
    
    // 2. 标签匹配
    if (StringUtils.hasText(tags)) {
        query.like(Document::getTags, tags);
    }
    
    // 3. 权限过滤 (伪代码)
    // List<Long> allowedDocIds = permissionService.getAccessibleDocIds(userId);
    // query.in(Document::getId, allowedDocIds);
    
    return Result.success(documentMapper.selectList(query));
}
```

### 3. 测试验证
- **测试用例 1 (组合搜索)**：输入关键词 "Project" 且标签选择 "Technical"，验证结果集准确性。
- **测试用例 2 (时间筛选)**：设置开始/结束时间，验证仅返回范围内的文档。
- **测试用例 3 (空结果)**：输入不存在的关键词，验证页面显示“暂无数据”而非报错。

## 2.4 文档导入与导出（选做）

### 1. 相关文件
- **前端**：
  - [useDocument.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/hooks/useDocument.ts): 包含导出 PDF/Markdown 的前端逻辑。
  - [documentApi.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/api/documentApi.ts): 定义导入导出 API 接口。
- **后端**：
  - [DocumentController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/DocumentController.java): 处理文件上传下载请求。
  - [DocumentServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/DocumentServiceImpl.java): 实现具体的 POI/PDFBox 处理逻辑。

### 2. 设计与实现

**多格式支持**：
- **Word (.docx/.doc)**：
  - **导入**：后端使用 **Apache POI** (`XWPFDocument` / `HWPFDocument`) 解析文档，提取纯文本内容存入数据库。
  - **导出**：后端创建 `XWPFDocument`，将数据库中的 content 写入段落，通过 `ByteArrayOutputStream` 输出为流。
- **PDF (.pdf)**：
  - **导入**：后端使用 **Apache PDFBox** (`PDFTextStripper`) 提取 PDF 文本内容。
  - **导出**：后端提供流式输出接口（当前实现为简化版，直接输出文本流；完整版需使用 PDFBox 生成带格式的 PDF 页面）。
- **Markdown (.md)**：
  - **导出**：纯前端实现。利用浏览器 `Blob` 对象将文本内容封装为 `text/markdown` 格式，触发下载。

**批量操作 (设计中)**：
- **批量导入**：
  - **前端**：文件上传组件开启 `multiple` 属性，将多个文件封装为 `FormData` 发送。
  - **后端**：接收 `MultipartFile[]` 数组，遍历文件，根据后缀名（.docx, .pdf）自动路由到对应的解析逻辑，返回成功/失败的统计结果。
- **批量导出**：
  - **设计**：前端传递 `docId` 列表 -> 后端创建 `ZipOutputStream` -> 循环生成单个文档文件并写入 Zip 条目 -> 返回压缩包流。

**核心代码 (DocumentServiceImpl.java - Word 导入)**：
```java
@Override
public Result<?> importWord(MultipartFile file, Long userId, String category) {
    try {
        String content = "";
        String fileName = file.getOriginalFilename();
        
        if (fileName.endsWith(".docx")) {
            // 处理 .docx (OOXML)
            try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph paragraph : doc.getParagraphs()) {
                    sb.append(paragraph.getText()).append("\n");
                }
                content = sb.toString();
            }
        } else if (fileName.endsWith(".doc")) {
            // 处理 .doc (OLE2)
            try (HWPFDocument doc = new HWPFDocument(file.getInputStream());
                 WordExtractor extractor = new WordExtractor(doc)) {
                content = extractor.getText();
            }
        }
        // ... 创建文档逻辑
        return Result.success(document);
    } catch (Exception e) {
        return Result.error("Word导入失败：" + e.getMessage());
    }
}
```

### 3. 测试验证
- **测试用例 1 (Word 导入)**：上传一个包含多段落的 `.docx` 文件，验证编辑器中是否正确显示文本内容。
- **测试用例 2 (PDF 导入)**：上传标准 PDF 文件，验证文本提取准确性（注意图片无法提取）。
- **测试用例 3 (Markdown 导出)**：在编辑器输入内容，点击“导出 Markdown”，检查下载的文件是否可用记事本打开且内容一致。

## 3.1 实时编辑与同步

### 1. 相关文件
- **前端**：
  - [Editor.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/Editor.tsx): 编辑器主入口。
  - [useCollaboration.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/hooks/useCollaboration.ts): 处理 WebSocket 连接与状态同步。
- **后端**：
  - [DocumentWebSocketHandler.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/websocket/DocumentWebSocketHandler.java): WebSocket 消息处理器。

### 2. 设计与实现

**多用户同步**：
- **WebSocket 通信**：建立长连接，实时推送 `content_update` 事件。
- **并发控制**：后端使用 `ConcurrentHashMap` 管理 `docId -> sessions` 映射，确保多房间隔离。
- **操作转换 (OT)**：采用 OT 算法解决多用户并发编辑冲突（`OTAlgorithm`）。

**光标同步**：
- **数据结构**：
  ```typescript
  interface CursorPosition {
    userId: number;
    username: string;
    position: number;
    length: number; // 选区长度
  }
  ```
- **广播机制**：
  1. 前端监听 Quill 编辑器选区变化 (`onDidChangeSelection`)。
  2. 发送 `cursor_position_update` 消息到后端。
  3. 后端转发给同一文档下的其他所有在线用户。
  4. 其他用户前端接收消息，利用 Quill API (`createCursor`) 在对应位置渲染带颜色的光标旗标。

**核心代码 (DocumentWebSocketHandler.java - 光标广播)**：
```java
private void handleCursorPosition(WebSocketSession session, Map<String, Object> messageMap, Long docId) throws IOException {
    // ... 解析参数
    Set<WebSocketSession> sessions = DOCUMENT_SESSIONS.get(docId);
    if (sessions != null) {
        // 构建消息
        Map<String, Object> response = new HashMap<>();
        response.put("type", "cursor_position_update");
        response.put("userId", userId);
        response.put("cursorPosition", cursorPosition);
        // ...
        
        // 广播给其他用户
        for (WebSocketSession otherSession : sessions) {
            if (!otherSession.equals(session)) {
                otherSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }
        }
    }
}
```

### 3. 测试验证
- **测试用例 1 (协同编辑)**：开启两个浏览器窗口登录不同账号进入同一文档，A 输入字符，B 应在 100ms 内看到变化。
- **测试用例 2 (光标可见性)**：A 选中一段文本，B 应看到 A 的名字和对应颜色的高亮选区。

## 3.2 评论与批注

### 1. 相关文件
- **前端**：[CommentPanel.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/CommentPanel.tsx)
- **后端**：[NotificationService](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/NotificationService.java) (推测)

### 2. 设计与实现

**功能模块**：
- **行内批注**：
  - 用户选中文本后创建评论，前端记录 `startPos`, `endPos` 和 `selectedText`。
  - 显示时，若文档内容未大幅变动，高亮对应位置（需配合 Quill 的 Annotation 模块优化位置追踪）。
- **回复机制**：
  - 评论数据结构包含 `parentId`。
  - 前端递归渲染或两层扁平渲染（当前为两层：根评论 + 回复列表）。
- **@提及**：
  - 前端正则匹配 `/@(\w+)/g`，将 `@username` 渲染为蓝色高亮链接。
  - 后端在创建评论时解析内容，若发现 @提及，自动为目标用户创建通知。

**核心代码 (CommentPanel.tsx - 渲染逻辑)**：
```typescript
const renderCommentItem = (comment: Comment, isReply = false) => {
  // 递归获取回复
  const replies = !isReply ? getReplies(comment.id) : [];
  return (
    <Box>
       {/* 评论主体 */}
       {/* 选中文本展示 */}
       {comment.selectedText && (
          <Typography variant="body2" sx={{ fontStyle: 'italic' }}>
            "{comment.selectedText}"
          </Typography>
       )}
       {/* 内容（含 @提及解析） */}
       <Typography>{parseMentions(comment.content)}</Typography>
       
       {/* 渲染回复列表 */}
       {replies.map(reply => renderCommentItem(reply, true))}
    </Box>
  );
};
```

### 3. 测试验证
- **测试用例 1 (添加批注)**：选中“项目计划”四个字添加评论，验证评论面板显示“针对选中文本: 项目计划”。
- **测试用例 2 (回复)**：点击某条评论的“回复”按钮，发送内容，验证其出现在原评论下方。
- **测试用例 3 (@通知)**：评论中输入 "@admin"，登录 admin 账号检查通知中心是否收到提醒。

## 3.3 任务分配与跟踪

### 1. 相关文件
- **前端**：[TaskPanel.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/TaskPanel.tsx)
- **后端**：[TaskServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/TaskServiceImpl.java)

### 2. 设计与实现

**功能模块**：
- **任务创建**：关联 `docId`，指定 `assigneeId`（负责人）和 `deadline`（截止日期）。
- **状态流转**：
  - 0: 待处理 (Pending)
  - 1: 进行中 (In Progress)
  - 2: 已完成 (Completed)
- **自动通知**：
  - 当任务状态变更或截止日期修改时，后端自动向 **创建者** 和 **负责人** 发送系统通知。

**核心代码 (TaskServiceImpl.java - 状态更新与通知)**：
```java
@Override
public boolean updateTaskStatus(Long taskId, Long userId, TaskUpdateDTO dto) {
    // ... 权限校验与更新逻辑
    
    // 状态或截止日期变化触发通知
    if (statusChanged || deadlineChanged) {
        String content = "任务\"" + task.getTitle() + "\"状态更新为" + statusText;
        
        // 通知负责人
        Notification notification = new Notification();
        notification.setUserId(task.getAssigneeId());
        notification.setType("task_status");
        notification.setContent(content);
        notificationService.create(notification);
        
        // 同步通知创建者...
    }
    return true;
}
```

### 3. 测试验证
- **测试用例 1 (任务分配)**：A 用户创建任务指派给 B，B 登录后在文档侧边栏应看到该任务。
- **测试用例 2 (状态变更)**：B 将任务标记为“已完成”，A 收到通知“您创建的任务...状态已更新为已完成”。

## 4.1 通知系统

### 1. 相关文件
- **前端**：
  - [NotificationPanel.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/NotificationPanel.tsx): 通知列表与操作面板。
  - [NotificationSettingModal.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/NotificationSettingModal.tsx): 通知偏好设置。
  - [notificationApi.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/api/notificationApi.ts): 前端 API 封装。
- **后端**：
  - [NotificationServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/NotificationServiceImpl.java): 通知业务逻辑与 WebSocket 推送。
  - [NotificationSettingController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/NotificationSettingController.java): 通知设置接口。

### 2. 设计与实现

**实时通知**：
- **WebSocket 推送**：后端 `NotificationServiceImpl` 在创建通知后，通过 `ChatWebSocketHandler.sendMessageToUser` 实时推送给目标用户。
- **轮询兜底**：前端 `NotificationPanel` 在组件加载时调用 `fetchNotifications` 拉取未读列表，并定时或在 WebSocket 断连时进行轮询。

**分类与过滤**：
- **类型支持**：支持 `mention` (@提及), `comment` (评论), `reply` (回复), `task` (任务), `collab` (协作), `file` (文件) 等多种类型。
- **前端过滤**：`NotificationPanel` 使用 `Tabs` 组件切换不同类型的通知视图。

**通知设置**：
- **个性化开关**：用户可在 `NotificationSettingModal` 中开启/关闭 @提及、任务分配、任务状态变更及邮件通知。
- **持久化**：设置存储在 `t_notification_setting` 表中，通过 `NotificationSettingService` 管理。

**核心代码 (NotificationServiceImpl.java - 实时推送)**：
```java
private void pushNotificationToUser(Notification notification) {
    try {
        String notificationJson = objectMapper.writeValueAsString(notification);
        ChatWebSocketHandler.sendMessageToUser(notification.getUserId(), notificationJson);
    } catch (Exception e) {
        logger.error("推送通知失败", e);
    }
}
```

### 3. 测试验证
- **测试用例 1 (实时性)**：用户 A 触发通知（如 @用户 B），用户 B 的铃铛图标应立即显示红点并弹出提示。
- **测试用例 2 (设置生效)**：用户 B 关闭“任务分配通知”，用户 A 分配任务给 B，B 不应收到通知推送（但在列表中可能仍可见或标记为不提醒）。

## 4.2 实时通讯

### 1. 相关文件
- **前端**：
  - [ChatPanel.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/ChatPanel.tsx): 聊天侧边栏。
  - [VideoMeetingModal.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/VideoMeetingModal.tsx): 视频会议与屏幕共享。
- **后端**：
  - [ChatWebSocketHandler.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/websocket/ChatWebSocketHandler.java): 聊天消息处理。
  - [VideoMeetingServiceImpl.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/service/impl/VideoMeetingServiceImpl.java): 会议管理。

### 2. 设计与实现

**内置聊天**：
- **即时消息**：基于 WebSocket 实现私聊与群聊（当前侧重于基于文档的上下文聊天或点对点私聊）。
- **文件共享**：支持发送文件，通过 `chatApi.sendFile` 上传至 MinIO，并在聊天窗口显示下载链接。
- **在线状态**：WebSocket 握手时记录在线用户，实时广播 `online_users` 列表。

**视频会议集成**：
- **Agora SDK**：集成 Agora RTC SDK (v4) 实现高质量音视频通话。
- **会议流程**：
  1. **创建/加入**：通过 `VideoMeetingModal` 发起会议，后端生成或验证 `meetingId` 和 `token`。
  2. **媒体控制**：支持开关麦克风、摄像头。
  3. **屏幕共享**：利用 Agora `createScreenVideoTrack` 实现屏幕共享流的发布。
  4. **多流订阅**：`RemoteVideoPlayer` 组件动态订阅并渲染远端用户的音视频流。

**核心代码 (VideoMeetingModal.tsx - Agora 初始化)**：
```typescript
const initAgora = async () => {
  const client = AgoraRTC.createClient({ mode: 'rtc', codec: 'vp8' });
  await client.join(appId, channel, token, uid);
  
  // 发布本地流
  const [mic, cam] = await AgoraRTC.createMicrophoneAndCameraTracks();
  await client.publish([mic, cam]);
  
  // 监听远端流
  client.on('user-published', async (user, mediaType) => {
    await client.subscribe(user, mediaType);
    if (mediaType === 'video') {
       // 更新远端用户列表触发渲染
    }
  });
};
```

### 3. 测试验证
- **测试用例 1 (聊天)**：发送文本和图片文件，接收方应实时收到并能预览/下载。
- **测试用例 2 (视频会议)**：两名用户加入同一文档的会议，确认能看到对方画面并听到声音。
- **测试用例 3 (屏幕共享)**：发起屏幕共享，远端用户应能清晰看到共享内容。

## 5.1 前端架构

## 6.1 系统监控（选做）

### 1. 相关文件
- **前端**：
  - [MonitorDashboard.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/pages/MonitorDashboard.tsx): 监控仪表盘，集成系统健康、错误日志、用户行为等通过 Tab 展示。
  - [monitorApi.ts](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/api/monitorApi.ts): 监控相关接口封装。
- **后端**：
  - [MonitorController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/MonitorController.java): 提供 CPU、内存、线程、磁盘及系统健康检查接口。
  - [ErrorLogController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/ErrorLogController.java): 错误日志查询接口。
  - [GlobalExceptionHandler.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/config/GlobalExceptionHandler.java): 全局异常捕获。

### 2. 设计与实现

**实时性能监控**：
- **数据指标**：
  - **JVM 内存**：堆内存使用情况（Total, Max, Free, Used）。
  - **CPU 使用率**：系统负载与进程负载，利用 `OperatingSystemMXBean` 获取。
  - **线程状态**：活跃线程数、峰值线程数、守护线程数。
  - **磁盘空间**：各分区的总量与剩余空间。
- **展示方式**：前端通过 `setInterval` 定时轮询（或手动刷新），使用进度条 (`LinearProgress`) 和卡片直观展示资源占用率。

**错误日志记录**：
- **捕获机制**：
  - **后端**：`GlobalExceptionHandler` 捕获所有未处理异常，记录堆栈信息并入库 `t_error_log`。
  - **前端**：`window.onerror` 和 `unhandledrejection` 捕获浏览器端错误，通过 API 上报。
- **日志字段**：`timestamp`, `level`, `message`, `stackTrace`, `userId`, `apiPath`。

**系统健康报告**：
- **健康检查接口** (`/api/monitor/health`)：聚合系统基础信息（OS, Java 版本）、运行时长 (Uptime) 及各子系统状态，返回 JSON 格式报告。

**核心代码 (MonitorController.java - 获取系统信息)**：
```java
@GetMapping("/system")
public Result<Map<String, Object>> getSystemInfo() {
    Map<String, Object> systemInfo = new LinkedHashMap<>();
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    
    systemInfo.put("osName", System.getProperty("os.name"));
    systemInfo.put("javaVersion", System.getProperty("java.version"));
    systemInfo.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new Date(runtimeMxBean.getStartTime())));
    systemInfo.put("uptime", formatUptime(runtimeMxBean.getUptime()));
    
    return Result.success(systemInfo);
}
```

## 6.2 用户管理

### 1. 相关文件
- **前端**：
  - [UserManagement.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/admin/UserManagement.tsx): 用户列表与角色管理。
  - [UserBehavior.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/admin/UserBehavior.tsx): 用户行为统计图表。
  - [SurveyStats.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/admin/SurveyStats.tsx): 满意度调查结果。
- **后端**：
  - [UserController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/UserController.java): 用户增删改查。
  - [UserActivityController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/UserActivityController.java): 行为数据接口。

### 2. 设计与实现

**用户列表与权限调整**：
- **列表管理**：管理员可分页查看所有注册用户（ID, 用户名, 邮箱, 角色, 注册时间）。
- **角色控制**：
  - 支持将用户角色修改为 `admin` (管理员), `editor` (普通用户), `viewer` (观察者)。
  - 修改后实时生效，前端根据角色控制菜单可见性（如“管理后台”入口）。

**用户行为分析**：
- **数据采集**：在关键业务点（登录、打开文档、导出、权限变更）调用 `UserActivityService.recordActivity`。
- **统计维度**：
  - **活跃用户 Top10**：基于操作频次排序。
  - **操作类型分布**：使用饼图展示不同操作（如 Document Update vs View）的比例。
  - **近期活动**：时间轴展示最新的用户操作记录。

**用户满意度调查**：
- **收集**：用户在个人中心或弹窗填写评分（1-5星）和建议。
- **统计**：后台计算平均分，并列表展示最新留言，辅助产品优化。

**核心代码 (UserActivityController.java - 获取统计)**：
```java
@GetMapping("/statistics")
public Result getUserActivityStatistics(...) {
    // 聚合查询活跃用户与操作分布
    Map<String, Object> stats = userActivityService.getUserActivityStatistics(userId, startTime, endTime);
    return Result.success(stats);
}
```

## 6.3 系统配置（选做）

### 1. 相关文件
- **前端**：
  - [NotificationSettingModal.tsx](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-frontend/src/components/NotificationSettingModal.tsx)
- **后端**：
  - [NotificationSettingController.java](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/java/com/collab/collab_editor_backend/controller/NotificationSettingController.java)
  - [application.properties](file:///e:/Multiplayer_Online_Compilation_Software/collab-editor-backend/src/main/resources/application.properties)

### 2. 设计与实现

**系统参数设置**：
- **静态配置**：通过 `application.properties` 管理数据库连接、Redis 地址、MinIO 端点、JWT 过期时间及 Agora SDK 密钥。
- **环境隔离**：支持 `spring.profiles.active=dev/prod` 切换不同环境配置。

**功能模块开关（用户级）**：
- **通知偏好**：用户可自定义开启/关闭以下模块的通知：
  - `@提及` (@Mention)
  - 任务分配 (Task Assign)
  - 任务状态变更 (Task Status)
  - 邮件通知 (Email)
- **实现逻辑**：前端开关变更 -> 调用 API 更新 `t_notification_setting` -> 后端推送通知前检查该表配置 -> 决定是否发送。

## 7.1 测试策略

### 1. 单元测试
- **框架**：JUnit 5 + Mockito。
- **覆盖范围**：
  - Service 层业务逻辑（如 `DocumentService` 的导入导出）。
  - Util 工具类（如 `SecurityUtil` 的 XSS 过滤）。

### 2. 集成测试
- **Postman/API 测试**：验证 Controller 层接口的输入输出，特别是边界条件（如分页参数、非法 ID）。
- **WebSocket 测试**：使用专门的 WebSocket 客户端工具验证消息推送和连接稳定性。

### 3. 系统测试
- **端到端流程**：模拟用户从注册 -> 登录 -> 创建文档 -> 邀请协作 -> 实时编辑 -> 导出文档的全流程。
- **多用户并发**：开启多个浏览器窗口模拟多人同时编辑同一文档，验证 OT 算法和光标同步。

## 7.2 部署方案

### 1. 环境准备
- **JDK**: 17+
- **Database**: MySQL 8.0+
- **Cache**: Redis 6.0+
- **Object Storage**: MinIO (本地或服务器部署)
- **Frontend Runtime**: Node.js 18+ (构建阶段), Nginx (运行阶段)

### 2. 部署步骤
1. **数据库初始化**：执行 `schema.sql` 创建表结构。
2. **后端构建**：`mvn clean package -DskipTests` 生成 JAR 包。
3. **前端构建**：`npm run build` 生成 `dist` 静态资源。
4. **服务启动**：
   - 启动 Redis 和 MinIO。
   - `java -jar collab-editor-backend.jar`
   - 配置 Nginx 指向 `dist` 目录并反向代理 `/api` 和 `/ws` 到后端端口。

## 8.1 总结与展望

## 已修复问题摘要
- 观察者打开文档自动登出
- 文档内容获取失败（统一包装 200 导致前端误判）
- recentActivities.map 报错（返回结构不匹配）
- 满意度调查提交后仍显示暂无数据（表缺失与类型不匹配）
- 权限面板字体颜色与风格统一、成功提示、连续分配体验
- 动态导入失败（组件导入语法错误）
- **权限体系增强**：
  - 后端增加严格的角色校验，禁止非管理员创建/删除文档。
  - 实施基于角色的权限分配约束（观察者只能查看，编辑者才可编辑）。
- **编辑器体验优化**：
  - 修复 WebSocket 消息回环导致的编辑器自动输入空格/光标乱跳问题。

## 后续优化建议
- 后端错误返回使用标准 HTTP 状态码，并在前端统一处理
- 将角色与权限术语统一（viewer/observer）
- 完善单元测试与端到端测试；集成 CI
- 调整 Axios 拦截器以统一携带 Token、处理错误信息

---
最后更新：2025-12-30
