# 实时协作编辑系统环境部署手册

## 1. 系统概述

实时协作编辑系统是一个基于前后端分离架构的在线文档协作平台，支持多用户同时编辑同一文档并实时同步。系统采用现代化技术栈，提供了文档的创建、编辑、保存和实时协作功能。

## 2. 环境要求

### 2.1 硬件要求
- CPU: 2核及以上
- 内存: 4GB及以上
- 硬盘: 50GB及以上可用空间

### 2.2 软件要求

| 软件名称 | 版本要求 | 用途 |
|----------|----------|------|
| JDK      | 17+      | 后端运行环境 |
| Node.js  | 16+      | 前端开发和构建环境 |
| Maven    | 3.6+     | 后端项目构建工具 |
| MySQL    | 8.0+     | 关系型数据库 |
| MinIO    | RELEASE.2024-11-06T22-51-00Z | 对象存储服务 |
| Git      | 2.0+     | 版本控制工具 |

## 3. 数据库配置

### 3.1 安装MySQL

#### 3.1.1 Windows系统
1. 下载MySQL安装包：`https://dev.mysql.com/downloads/installer/`
2. 运行安装程序，选择「Custom」安装类型
3. 选择需要安装的MySQL版本（推荐8.0+）
4. 按照安装向导完成安装，设置root密码
5. 确保MySQL服务已启动

#### 3.1.2 Linux系统
以Ubuntu为例：
```bash
# 更新软件包列表
sudo apt update

# 安装MySQL
sudo apt install mysql-server

# 启动MySQL服务
sudo systemctl start mysql

# 设置开机自启
sudo systemctl enable mysql

# 配置MySQL安全选项
sudo mysql_secure_installation
```

### 3.2 创建数据库

1. 登录MySQL：
   ```bash
   mysql -u root -p
   ```

2. 创建数据库：
   ```sql
   CREATE DATABASE collab_editor DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. 创建用户并授权：
   ```sql
   CREATE USER 'collab_user'@'localhost' IDENTIFIED BY 'collab_password';
   GRANT ALL PRIVILEGES ON collab_editor.* TO 'collab_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

### 3.3 数据库表结构

系统启动时会自动创建所需的表结构（使用Spring Boot JPA的自动建表功能）。

## 4. MinIO配置

### 4.1 下载和安装MinIO

#### 4.1.1 Windows系统
1. 下载MinIO服务器：`https://dl.min.io/server/minio/release/windows-amd64/minio.exe`
2. 创建MinIO数据目录：`mkdir D:\minio-data`
3. 启动MinIO服务器：
   ```bash
   minio.exe server D:\minio-data --console-address :9001
   ```

#### 4.1.2 Linux系统
```bash
# 下载MinIO
wget https://dl.min.io/server/minio/release/linux-amd64/minio

# 设置可执行权限
chmod +x minio

# 创建数据目录
mkdir -p ~/minio-data

# 启动MinIO
./minio server ~/minio-data --console-address :9001
```

### 4.2 配置MinIO

1. 访问MinIO控制台：`http://localhost:9001`
2. 使用默认凭证登录（用户名：minioadmin，密码：minioadmin）
3. 创建存储桶：
   - 点击「Create Bucket」
   - 输入桶名称：`collab-editor-bucket`
   - 点击「Create Bucket」完成创建
4. 创建访问密钥：
   - 点击左侧导航栏的「Access Keys」
   - 点击「Create access key」
   - 保存生成的「Access Key」和「Secret Key」

## 5. 后端部署

### 5.1 克隆项目代码

```bash
git clone <项目仓库地址>
cd collab-editor-backend/collab-editor-backend
```

### 5.2 配置文件修改

编辑 `src/main/resources/application.properties` 文件：

```properties
# 应用配置
server.port=8080

# 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/collab_editor?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
spring.datasource.username=collab_user
spring.datasource.password=collab_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA配置
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# MinIO配置
minio.url=http://localhost:9000
minio.access-key=your-minio-access-key
minio.secret-key=your-minio-secret-key
minio.bucket-name=collab-editor-bucket

# JWT配置
jwt.secret=your-jwt-secret-key
jwt.expiration=3600000
jwt.header=Authorization

# CORS配置
spring.web.cors.allowed-origins=http://localhost:5173
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
spring.web.cors.allowed-headers=*
spring.web.cors.allow-credentials=true
```

### 5.3 构建项目

```bash
# 构建项目
mvn clean package -DskipTests

# 构建成功后，生成的jar包位于target目录
ls target/collab-editor-backend-*.jar
```

### 5.4 运行项目

#### 5.4.1 开发环境运行

```bash
# 直接运行
mvn spring-boot:run

# 或使用内置Maven wrapper
./mvnw spring-boot:run
```

#### 5.4.2 生产环境运行

```bash
# 运行jar包
java -jar target/collab-editor-backend-1.0.0.jar

# 或使用nohup后台运行
nohup java -jar target/collab-editor-backend-1.0.0.jar > app.log 2>&1 &
```

### 5.5 验证后端服务

访问 `http://localhost:8080/actuator/health`，如果返回以下结果，说明后端服务已正常启动：

```json
{
  "status": "UP"
}
```

## 6. 前端部署

### 6.1 克隆项目代码

```bash
git clone <项目仓库地址>
cd collab-editor-frontend
```

### 6.2 安装依赖

```bash
# 安装项目依赖
npm install
```

### 6.3 配置文件修改

编辑 `.env` 文件（如果不存在则创建）：

```env
# 开发环境配置
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080/ws/collab
```

### 6.4 构建项目

#### 6.4.1 开发环境构建

```bash
# 开发环境运行
npm run dev
```

访问 `http://localhost:5173` 即可查看前端应用。

#### 6.4.2 生产环境构建

```bash
# 构建生产版本
npm run build

# 构建后的文件位于dist目录
ls dist/
```

### 6.5 部署到Nginx

#### 6.5.1 安装Nginx

##### 6.5.1.1 Windows系统
1. 下载Nginx：`https://nginx.org/en/download.html`
2. 解压到指定目录，如 `D:\nginx`
3. 启动Nginx：双击 `nginx.exe`

##### 6.5.1.2 Linux系统
以Ubuntu为例：
```bash
sudo apt update
sudo apt install nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

#### 6.5.2 配置Nginx

编辑Nginx配置文件：

```nginx
server {
    listen 80;
    server_name localhost;

    location / {
        root /path/to/your/collab-editor-frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API代理配置
    location /api {
        proxy_pass http://localhost:8080/api;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }

    # WebSocket代理配置
    location /ws {
        proxy_pass http://localhost:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

#### 6.5.3 重启Nginx

```bash
# Windows系统
nginx -s reload

# Linux系统
sudo systemctl reload nginx
```

访问 `http://localhost` 即可查看部署后的前端应用。

## 7. 系统启动

### 7.1 启动顺序

1. 启动MySQL数据库
2. 启动MinIO对象存储服务
3. 启动后端服务
4. 启动前端服务（或通过Nginx访问）

### 7.2 访问系统

1. 打开浏览器，访问：`http://localhost:5173`（开发环境）或 `http://localhost`（生产环境）
2. 点击「注册」按钮，创建新用户
3. 使用新创建的用户登录系统
4. 开始使用实时协作编辑功能

## 8. 常见问题及解决方案

### 8.1 数据库连接失败

**问题现象**：后端启动时出现数据库连接错误

**解决方案**：
1. 检查MySQL服务是否已启动
2. 检查`application.properties`中的数据库配置是否正确
3. 确保数据库用户有足够的权限
4. 检查防火墙是否允许MySQL端口（默认3306）的连接

### 8.2 MinIO连接失败

**问题现象**：后端启动时出现MinIO连接错误

**解决方案**：
1. 检查MinIO服务是否已启动
2. 检查`application.properties`中的MinIO配置是否正确
3. 确保MinIO存储桶已创建
4. 检查访问密钥和密钥是否正确

### 8.3 前端无法连接后端API

**问题现象**：前端控制台出现API请求错误

**解决方案**：
1. 检查后端服务是否已启动
2. 检查前端`.env`文件中的API基础URL是否正确
3. 检查浏览器控制台的网络请求，确认请求URL是否正确
4. 检查CORS配置是否正确

### 8.4 WebSocket连接失败

**问题现象**：编辑器中无法实现实时同步

**解决方案**：
1. 检查WebSocket服务器是否已启动
2. 检查前端`.env`文件中的WebSocket基础URL是否正确
3. 检查浏览器是否支持WebSocket
4. 检查网络环境是否允许WebSocket连接

### 8.5 文档保存失败

**问题现象**：编辑文档后保存失败

**解决方案**：
1. 检查后端服务是否正常运行
2. 检查MinIO服务是否正常运行
3. 检查文档内容是否过大
4. 查看后端日志，了解具体错误原因

## 9. 系统维护

### 9.1 日志查看

#### 9.1.1 后端日志

```bash
# 开发环境日志（在后端项目目录下）
mvn spring-boot:run

# 生产环境日志
# 如果使用nohup运行
cat app.log
```

#### 9.1.2 前端日志

通过浏览器的开发者工具（F12）的「Console」和「Network」选项卡查看前端日志和网络请求。

### 9.2 数据库备份

```bash
# 备份数据库
mysqldump -u collab_user -p collab_editor > collab_editor_backup.sql

# 恢复数据库
mysql -u collab_user -p collab_editor < collab_editor_backup.sql
```

### 9.3 性能监控

- 后端可以通过Spring Boot Actuator进行监控：`http://localhost:8080/actuator`
- 数据库可以使用MySQL Workbench或其他数据库管理工具进行监控
- MinIO可以通过控制台监控存储使用情况：`http://localhost:9001`

## 10. 更新系统

### 10.1 更新后端

```bash
# 拉取最新代码
git pull

# 构建新项目
mvn clean package -DskipTests

# 停止旧服务
# 如果使用nohup运行
kill $(ps -ef | grep 'collab-editor-backend' | grep -v grep | awk '{print $2}')

# 启动新服务
java -jar target/collab-editor-backend-1.0.0.jar
```

### 10.2 更新前端

```bash
# 拉取最新代码
git pull

# 安装新依赖
npm install

# 构建生产版本
npm run build

# 重启Nginx
nginx -s reload
```

## 11. 技术支持

如果在部署过程中遇到问题，可以通过以下方式获取技术支持：

- 查看项目文档：`README.md`
- 查看API文档：`api_documentation.md`
- 查看错误日志，定位问题原因
- 联系项目开发团队

## 12. 版本历史

| 版本 | 日期       | 更新内容               |
|------|------------|------------------------|
| 1.0  | 2024-12-13 | 初始版本，完成基础部署 |
