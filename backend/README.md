# 论坛后端

`backend/` 是校园论坛的 Spring Boot 后端服务，负责账号体系、JWT 鉴权、数据库访问、帖子与评论、点赞通知、文件上传、管理员权限和启动迁移。

默认地址：

```text
http://localhost:3000
```

健康检查：

```text
http://localhost:3000/health
```

## 技术栈

- Spring Boot
- Spring Security
- JWT
- MyBatis Plus
- MySQL
- Spring Mail
- Maven

## 主要能力

- 账号注册、邮箱验证码、账号密码登录。
- JWT 登录态校验。
- 用户资料、头像上传、密码和账号修改。
- 发帖、编辑、删除、图片上传、公开/私密权限。
- 评论、评论回复、评论删除。
- 点赞、取消点赞、点赞数量统计。
- 站内通知、未读数量、已读处理。
- 分区、热门分区、热度榜、置顶帖。
- 屏蔽用户和屏蔽分区。
- 管理后台接口，包括用户、帖子、评论和分区管理。
- 启动时自动补表、补字段，并确保默认站长账号存在。

## 数据库

默认数据库：

```text
kob
```

默认连接信息：

```text
用户：通过 `DB_USERNAME` 配置，开发默认 `root`
密码：通过 `DB_PASSWORD` 配置，默认留空
地址：通过 `DB_URL` 配置，开发默认 `localhost:3306/kob`
```

初始化 SQL：

```text
sql/forum_schema.sql
```

首次初始化：

```powershell
mysql -u你的用户名 -p < sql/forum_schema.sql
```

后端启动时会自动检查并创建或补充以下数据结构：

- `user`
- `post`
- `comment`
- `agree`
- `notification`
- `section`
- `block_rule`
- `post_view_record`

默认站长账号：

```text
账号：admin
密码：由 `ADMIN_PASSWORD` 环境变量决定；未设置时开发默认值为 `admin123456`
```

## 配置说明

配置文件：

```text
src/main/resources/application.properties
```

关键配置：

```properties
server.port=3000
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/kob?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8}
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=45MB
```

邮件验证码配置使用环境变量：

```properties
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

PowerShell 示例：

```powershell
$env:MAIL_HOST="smtp.qq.com"
$env:MAIL_PORT="465"
$env:MAIL_USERNAME="你的发件邮箱"
$env:MAIL_PASSWORD="邮箱 SMTP 授权码"
```

如果不配置 SMTP，注册邮箱验证码和账号修改验证码会发送失败。

## 文件上传

帖子图片和用户头像会保存在：

```text
uploads/
```

对外访问路径：

```text
http://localhost:3000/uploads/...
```

上传限制：

- 帖子图片：最多 9 张，单张最大 5MB。
- 头像：单张最大 2MB。
- 支持 `jpg`、`jpeg`、`png`、`webp`、`gif`。

## 启动后端

推荐使用脚本：

```powershell
.\run-backend.ps1
```

推荐先在本机设置环境变量：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的数据库密码"
$env:ADMIN_PASSWORD="你的管理员初始密码"
$env:JWT_SECRET="请替换为足够长的随机字符串"
```

脚本行为：

- 如果本机设置了 `JAVA_HOME`，自动加入 Java 路径。
- 使用 `target/backend-0.0.1-SNAPSHOT.jar` 启动后端。
- 如果 jar 不存在，则回退到 Maven Spring Boot 启动。
- 启动时读取 `DB_USERNAME`、`DB_PASSWORD`、`DB_URL` 等环境变量。

手动 jar 启动：

```powershell
java -jar target\backend-0.0.1-SNAPSHOT.jar --spring.datasource.password=你的数据库密码
```

## 构建

```powershell
.\mvnw.cmd clean package -DskipTests
```

构建产物：

```text
target/backend-0.0.1-SNAPSHOT.jar
```

如果构建时提示无法删除 jar，说明后端正在运行。先停止服务：

```powershell
cd ..
.\stop-all.ps1
```

## 接口分组概览

账号相关：

- `POST /user/account/token/`
- `POST /user/account/register/`
- `GET /user/account/info/`
- `POST /user/account/update/`
- `POST /user/account/avatar/upload/`
- `POST /user/account/email/code/`
- `POST /user/account/account/code/`
- `POST /user/account/account/update/`

帖子相关：

- `GET /user/post/get/`
- `GET /user/post/detail/`
- `GET /user/post/user/`
- `GET /user/post/mine/`
- `GET /user/post/hot/`
- `GET /user/post/pinned/`
- `POST /user/post/write/`
- `POST /user/post/update/`
- `POST /user/post/delete/`
- `POST /user/post/view/`

评论相关：

- `GET /user/post/getComment/`
- `POST /user/post/comment/`
- `POST /user/post/comment/delete/`

点赞相关：

- `GET /user/agree/add/`
- `GET /user/agree/delete/`
- `GET /user/agree/count/`
- `GET /user/agree/get/`

通知相关：

- `GET /user/notification/list/`
- `GET /user/notification/unread-count/`
- `POST /user/notification/read/`
- `POST /user/notification/readAll/`

分区和屏蔽：

- `GET /user/section/list/`
- `GET /user/section/hot/`
- `GET /user/block/list/`
- `POST /user/block/add/`
- `POST /user/block/delete/`

管理员：

- `GET /admin/users/`
- `POST /admin/users/status/`
- `POST /admin/users/role/`
- `POST /admin/users/custom-title/`
- `POST /admin/users/reset-profile/`
- `GET /admin/posts/`
- `POST /admin/posts/delete/`
- `POST /admin/posts/pin/`
- `GET /admin/comments/`
- `POST /admin/comments/delete/`
- `POST /admin/sections/create/`
- `POST /admin/sections/delete/`

## 权限说明

- 未登录用户可以浏览公开帖子、热榜、分区和评论。
- 登录用户可以发帖、点赞、评论、回复、屏蔽和管理自己的资料。
- 私密帖仅作者和管理员角色可见。
- 管理员角色包括 `ADMIN`、`SUPER_ADMIN`、`OWNER`。
- 默认 `admin` 账号会被修复为 `OWNER`。

## 常见问题

### MySQL 连接失败

检查：

- MySQL 服务是否启动。
- 是否存在 `kob` 数据库。
- `DB_USERNAME`、`DB_PASSWORD`、`DB_URL` 是否正确。
- `application.properties` 中数据库地址是否正确。

### 验证码邮件发送失败

检查：

- 是否设置 `MAIL_USERNAME` 和 `MAIL_PASSWORD`。
- 授权码是否正确。
- 邮箱服务商是否允许 SMTP。
- 防火墙是否拦截 SMTP 端口。

### 登录后接口返回 403

可能原因：

- JWT 过期。
- 用户被禁用。
- 前端仍使用旧 token。
- 访问了需要管理员角色的接口。

可尝试清除浏览器本地登录状态后重新登录。

### Maven 依赖下载失败

首次构建需要访问 Maven Central。网络受限时，先保证本机 `.m2` 缓存完整，或在可联网环境完成一次构建。

### 后端启动后中文乱码

项目文件按 UTF-8 保存。PowerShell 终端显示乱码时，可尝试：

```powershell
chcp 65001
```
