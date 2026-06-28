# 校园论坛

校园论坛是一个前后端分离的论坛网站，面向校园场景提供注册登录、邮箱验证、发帖、分区、图片上传、点赞、评论回复、站内通知、个人主页和管理员后台等功能。

项目默认运行在本机：

- 前端：`http://localhost:8081`
- 后端：`http://localhost:3000`
- 管理后台：`http://localhost:8081/#/admin`

## 技术栈

- 后端：Spring Boot、Spring Security、JWT、MyBatis Plus、MySQL、Spring Mail
- 前端：Vue 3、Vue Router、Vuex、Element Plus、Bootstrap、jQuery
- 数据库：MySQL，默认库名 `kob`
- 脚本环境：Windows PowerShell、Linux Bash、Docker Compose

## 项目结构

```text
campus-forum/
  backend/          Spring Boot 后端服务
  space/            Vue 3 前端项目
  .env.example      本地统一配置示例
  start-all.ps1     Windows 统一入口：配置、启动、关闭、Docker
  start-all.sh      Linux 统一入口：配置、启动、关闭、Docker
  docker-compose.yml Docker 编排配置
  README.md         项目总说明
```

更多说明：

- 后端文档：`backend/README.md`
- 前端文档：`space/README.md`
- 初始化 SQL：`backend/sql/forum_schema.sql`

## 核心功能

- 账号注册、邮箱验证码、账号密码登录、JWT 鉴权。
- 用户可修改用户名、头像、密码和账号。
- 发帖支持标题、正文、分区、图片、公开/私密权限。
- 帖子详情独立页面展示完整正文和评论。
- 点赞、评论、评论回复、评论删除。
- 站内通知，包括点赞、评论和删帖提醒。
- 个人主页展示用户公开信息和可查看帖子。
- 社区支持分区筛选、模糊搜索、屏蔽用户/分区。
- 热度榜、置顶帖、热门分区。
- 管理后台支持用户、帖子、评论、分区、置顶和权限管理。

## 默认账号

系统启动时会自动修复或创建默认站长账号：

```text
账号：admin
密码：由 `ADMIN_PASSWORD` 环境变量决定；未设置时开发默认值为 `admin123456`
角色：站长
```

如果曾手动清空数据库，需要重新启动后端，让启动迁移逻辑重新创建默认账号。

## 环境准备

1. 安装 JDK。后端脚本会优先使用本机已有的 `JAVA_HOME`，不要在仓库中写入个人电脑的绝对路径。

2. 安装 MySQL，并准备一个可连接 `kob` 数据库的账号。建议用 `.env` 中的 `DB_USERNAME`、`DB_PASSWORD`、`DB_HOST`、`DB_PORT`、`DB_NAME` 配置；需要完全自定义 JDBC 地址时再使用 `DB_URL`。

3. 安装 Node.js 和 npm。

4. 首次运行直接执行统一启动脚本。脚本会先检查 `.env`，不存在时自动进入配置向导：

   ```powershell
   .\start-all.ps1
   ```

   脚本会检查 Java、Node.js、npm、MySQL 命令，按提示生成本机 `.env`，并提示数据库初始化、前端依赖安装和启动方式。`.env` 已加入忽略列表，不要提交到仓库。

5. 如果没有通过配置向导安装依赖，首次运行前手动安装前端依赖：

   ```powershell
   cd space
   npm install --legacy-peer-deps
   ```

## 数据库初始化

数据库名固定为 `kob`。可以使用项目 SQL 初始化：

```powershell
mysql -u你的用户名 -p < backend/sql/forum_schema.sql
```

后端启动时还会自动补充缺失字段和表，例如用户角色、帖子图片、分区、通知、屏蔽规则和浏览记录表。旧数据库升级时一般只需要启动新后端即可完成迁移。

## 邮箱验证码配置

注册和账号修改验证码依赖 SMTP。推荐用本地环境变量配置，不要把真实授权码提交到仓库。

PowerShell 示例：

```powershell
$env:MAIL_HOST="smtp.qq.com"
$env:MAIL_PORT="465"
$env:MAIL_USERNAME="你的发件邮箱"
$env:MAIL_PASSWORD="邮箱 SMTP 授权码"
```

也可以复制示例文件为本地统一配置 `.env`，里面可同时配置前后端端口、数据库、管理员密码、JWT 密钥和 SMTP。`start-all.ps1` 会自动加载它。`.env` 已加入忽略列表，不要提交：

```powershell
Copy-Item .\.env.example .\.env
```

修改 `.env` 后需要重新执行 `.\start-all.ps1` 或 `./start-all.sh`。旧版 `local.config.ps1` 和 `mail.local.ps1` 不再作为推荐配置入口。

未配置或配置错误时，验证码接口会返回邮件发送失败。

## 一键启动和关闭

### Windows PowerShell

在项目根目录运行：

```powershell
.\start-all.ps1
```

启动行为：

- 后端使用 `backend/run-backend.ps1` 启动，默认端口 `3000`，可在 `.env` 中通过 `BACKEND_PORT` 修改。
- 后端控制台会显示出来，便于查看运行日志。
- 前端使用 `space/run-space.ps1` 启动，默认端口 `8081`，可在 `.env` 中通过 `FRONTEND_PORT` 修改。
- 前端会根据 `BACKEND_PORT` 自动设置后端接口地址。
- 前端日志会写入 `logs/`。

关闭服务：

```powershell
.\start-all.ps1 stop
```

重新配置或重启：

```powershell
.\start-all.ps1 config
.\start-all.ps1 restart
```

启动后如果手动关闭后端命令窗口，隐藏监控进程会自动关闭前端服务。

### Linux Bash

首次运行先赋予脚本执行权限：

```bash
chmod +x start-all.sh backend/run-backend.sh space/run-space.sh
```

启动：

```bash
./start-all.sh
```

关闭：

```bash
./start-all.sh stop
```

重新配置或重启：

```bash
./start-all.sh config
./start-all.sh restart
```

Linux 脚本同样读取 `.env`，日志写入 `logs/`，进程号写入 `logs/backend.pid` 和 `logs/frontend.pid`。默认启动为前台控制模式，请保持终端打开；按 `Ctrl+C` 或关闭终端会一并关闭前后端。

### Docker Compose

先复制配置文件并填写真实密码、JWT 和邮箱授权码：

```bash
cp .env.example .env
```

Docker 前台启动。关闭终端或按 `Ctrl+C` 会执行 `docker compose down`：

```bash
./start-all.sh docker-start
```

Windows PowerShell 使用：

```powershell
.\start-all.ps1 docker-start
```

后台启动和关闭：

```bash
./start-all.sh docker-start --detached
./start-all.sh docker-stop
```

Windows PowerShell 后台模式：

```powershell
.\start-all.ps1 docker-start -Detached
.\start-all.ps1 docker-stop
```

清理数据库和上传文件 volume：

```bash
docker compose down -v
```

Docker 前端通过 nginx 暴露在 `http://localhost:${FRONTEND_PORT}`，浏览器请求 `/api/` 时会自动代理到后端容器。

## 单独启动后端

```powershell
cd backend
.\run-backend.ps1
```

后端健康检查：

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:3000/health
```

## 单独启动前端

```powershell
cd space
.\run-space.ps1
```

也可以手动运行：

```powershell
npm run serve -- --host 0.0.0.0 --port 8081
```

使用脚本启动时，前端端口和后端接口地址会自动读取 `.env`。

## 构建验证

后端构建：

```powershell
cd backend
.\mvnw.cmd clean package -DskipTests
```

前端构建：

```powershell
cd space
npm run build
```

前端构建产物在 `space/dist/`，后端 jar 在 `backend/target/backend-0.0.1-SNAPSHOT.jar`。

## 常见问题

### 前后端端口被占用

先执行：

```powershell
.\start-all.ps1 stop
```

如果仍被占用，可以在 `.env` 中修改 `BACKEND_PORT` 或 `FRONTEND_PORT`，再重新执行 `.\start-all.ps1`。也可以使用 `netstat -ano | Select-String ":端口号"` 查看进程。

### 后端出现 Whitelabel Error Page 或 403

常见原因：

- 访问了需要登录的接口。
- JWT 过期或本地登录状态失效。
- 后端未使用最新 jar 启动。
- 数据库中用户状态为 `DISABLED`。

可以先退出登录，再重新登录默认账号。账号为 `admin`，密码由 `ADMIN_PASSWORD` 环境变量决定；未设置时开发默认值为 `admin123456`。

### 邮件验证码发送失败

检查：

- 是否配置 `MAIL_USERNAME` 和 `MAIL_PASSWORD`。
- 邮箱是否开启 SMTP。
- `MAIL_PASSWORD` 是否为 SMTP 授权码，不是邮箱登录密码。
- 端口和协议是否与邮箱服务商要求一致。

### 图片或头像加载失败

上传文件默认保存在：

```text
backend/uploads/
```

后端通过 `/uploads/**` 对外提供访问。请确认后端正在运行，且前端访问的后端地址与 `.env` 中的 `BACKEND_PORT` 一致。

### 管理员登录失败

默认登录使用“账号”而不是展示用户名：

```text
账号：admin
密码：由 `ADMIN_PASSWORD` 环境变量决定；未设置时开发默认值为 `admin123456`
```

如果密码仍错误，可重启后端触发默认管理员修复逻辑。

### Maven 构建失败

常见原因：

- 首次构建需要联网下载依赖。
- 后端 jar 正在运行导致 `clean` 无法删除 `target`。
- JDK 路径与 `run-backend.ps1` 不一致。

如果 jar 被占用，先运行：

```powershell
.\start-all.ps1 stop
```
