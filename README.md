# VideoHub · media-manager

一个前后端分离的视频管理与播放系统，包含视频上传、在线播放、分页检索、分类筛选、动态排序、点赞、逻辑删除、封面展示、登录鉴权、缓存加速和链路日志。

- 后端：Spring Boot 4.1 + 原生 MyBatis + MySQL 8 + Caffeine + Logback
- 前端：Vue 3 + Vite + Axios
- 后端端口：`8080`
- 前端端口：`5173`
- 数据库：`video_db`
- 上传目录：`./uploads/`，封面目录：`./uploads/covers/`

## 一、功能

- 视频上传：扩展名白名单、UUID 重命名、自动创建目录、上传进度条。
- 视频列表：展示标题、简介、分类、标签、大小、格式、时长、播放量、点赞数、上传者、上传时间。
- 在线播放：支持 HTTP Range、拖动进度条、断点续播。
- 视频检索：标题和简介关键字搜索、分类筛选、多字段排序。
- 视频互动：播放量累加、点赞去重和取消点赞。
- 视频删除：数据库逻辑删除，磁盘文件在事务提交后清理。
- 视频封面：上传时可选封面，未选择时自动提取首帧，卡片和播放器展示封面，加载失败自动回退。
- 用户体系：注册、登录、JWT 鉴权、资源归属校验、登录失败限流。
- 缓存加速：分页、详情、分类、热门使用 Caffeine 缓存。
- 日志追踪：分级滚动日志，每个请求带 X-Request-Id 链路标识。

## 二、技术栈

| 层次 | 技术 |
| --- | --- |
| 运行环境 | JDK 21、Maven 3.9+、Node.js 22+ |
| 后端框架 | Spring Boot 4.1、原生 MyBatis |
| 数据库 | MySQL 8 |
| 缓存 | Spring Cache、Caffeine |
| 日志 | Logback、SLF4J、MDC |
| 安全 | BCrypt、HS256 JWT、HandlerInterceptor |
| 前端 | Vue 3、Vite、Axios |

## 三、环境要求

- JDK 21 及以上
- Maven 3.9 及以上（项目自带 `mvnw.cmd`）
- Node.js 22.18 及以上、npm 10 及以上
- MySQL 8.x

## 四、项目结构

```text
media-manager/
├─ src/main/java/pre/wc/mediamanager/
│  ├─ auth/dto/             登录、注册、用户 VO
│  ├─ cache/                缓存日志装饰器
│  ├─ common/               Result、PageResult、业务与鉴权异常
│  ├─ config/               CORS、缓存、异常、密码编码器、日志过滤器
│  ├─ controller/           Video、Auth、Media、Info 接口
│  ├─ entity/               Video、User、DTO/VO
│  ├─ mapper/               MyBatis Mapper
│  ├─ security/             JWT、拦截器、UserContext、登录限流、traceId
│  ├─ service/impl/         视频与认证业务实现
│  └─ util/                 视频元数据解析
├─ src/main/resources/
│  ├─ application.yml       数据源、上传目录、JWT、CORS
│  ├─ logback-spring.xml    分级滚动日志
│  ├─ mapper/               VideoMapper.xml、UserMapper.xml
│  └─ static/covers/        演示视频默认封面
├─ video-frontend/          Vue 3 前端
├─ scripts/verify.ps1       一键验收脚本
├─ docs/evidence/           验收输出与截图
│  （schema.sql / data.sql / migration.sql 已移至上级“实训4”提交目录，不放在项目内）
└─ README.md
```

## 五、数据库初始化

### 1. 全新初始化

> 数据库脚本已移到上级“实训4”提交目录，请在提交目录下执行以下命令。

```powershell
Get-Content -Raw ..\schema.sql | mysql -u root -p --default-character-set=utf8mb4
Get-Content -Raw ..\data.sql | mysql -u root -p --default-character-set=utf8mb4
```

### 2. 已有数据库迁移

```powershell
Get-Content -Raw ..\migration.sql | mysql -u root -p --default-character-set=utf8mb4
```

`migration.sql` 可以重复执行，会自动补齐 `app_user`、`video_like` 表和 `video` 表的归属、封面字段，并创建对应索引和外键。

### 3. 数据表

| 表 | 说明 |
| --- | --- |
| `app_user` | 用户账号，保存 BCrypt 密码摘要、昵称、角色、状态 |
| `video` | 视频信息，包含归属用户 `owner_id`、封面字段、播放量、点赞数、逻辑删除标记 |
| `video_like` | 点赞记录，`user_id + video_id` 唯一索引，保证同一用户只能点赞一次 |

### 4. 默认管理员

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | ADMIN |

该账号仅用于本地演示，部署前请修改密码和 `JWT_SECRET`。

## 六、配置说明

配置文件：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/video_db?...
    username: root
    password: ${DB_PASSWORD:AHxs5702}
  servlet:
    multipart:
      max-file-size: 1024MB
      max-request-size: 1024MB

server:
  port: 8080

video:
  upload-path: ${VIDEO_UPLOAD_PATH:./uploads/}
  cover-path: ${VIDEO_COVER_PATH:./uploads/covers/}

app:
  jwt:
    secret: ${JWT_SECRET:请在生产环境替换至少 32 字节的随机密钥}
    expiration-minutes: ${JWT_EXPIRATION_MINUTES:120}
  cors:
    allowed-origins: ${APP_CORS_ORIGINS:http://localhost:5173,http://127.0.0.1:5173}
```

常用环境变量：

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "video_db"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的密码"
$env:VIDEO_UPLOAD_PATH = "./uploads/"
$env:VIDEO_COVER_PATH = "./uploads/covers/"
$env:JWT_SECRET = "至少 32 字节的随机字符串"
```

## 七、启动

### 后端

```powershell
.\mvnw.cmd spring-boot:run
```

启动成功标志：

```text
Tomcat started on port 8080
Started MediaManagerApplication
```

如果 8080 被占用：

```powershell
Get-NetTCPConnection -State Listen -LocalPort 8080
Stop-Process -Id <PID> -Force
```

### 前端

```powershell
cd video-frontend
npm install
npm run dev
```

浏览器访问 <http://localhost:5173>。如果后端地址不同：

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev
```

### 测试与构建

```powershell
.\mvnw.cmd test
cd video-frontend
npm run build
```

## 八、接口列表

### 认证接口

| 方法 | 地址 | 访问 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 公开 | 注册并返回登录凭证 |
| POST | `/api/auth/login` | 公开 | 登录并返回 JWT |
| GET | `/api/auth/me` | 受保护 | 获取当前登录用户 |
| POST | `/api/auth/logout` | 受保护 | 退出，前端清除本地 token |

除登录、注册外，其余接口通过请求头携带凭证：

```http
Authorization: Bearer <token>
```

### 视频接口

| 方法 | 地址 | 访问 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/video/page` | 公开 | 分页查询：page、size、keyword、category、sortBy、order |
| GET | `/api/video/{id}` | 公开 | 视频详情 |
| GET | `/api/video/categories` | 公开 | 分类列表 |
| GET | `/api/video/hot?limit=6` | 公开 | 热门视频 |
| GET | `/api/video/stream/{fileName}` | 公开 | 视频流播放，支持 HTTP Range |
| GET | `/api/video/cover/{fileName}` | 公开 | 读取封面图片 |
| POST | `/api/video/upload` | 受保护 | 上传视频，可选封面 |
| POST | `/api/video/{id}/cover` | 受保护 | 更换封面 |
| POST | `/api/video/{id}/play` | 公开 | 播放量 +1 |
| POST | `/api/video/{id}/like` | 受保护 | 点赞切换：未点赞则点赞，已点赞则取消 |
| DELETE | `/api/video/{id}` | 受保护 | 逻辑删除视频 |

上传参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `file` | 是 | mp4 / webm / mkv / mov / avi |
| `cover` | 否 | JPG / PNG / WebP，不超过 5MB |
| `title` | 否 | 留空时使用原文件名 |
| `description` | 否 | 最长 500 字符 |
| `category` | 否 | 默认“默认” |
| `tags` | 否 | 逗号分隔 |
| `duration` | 否 | 前端解析的时长，后端解析失败时兜底 |

统一返回结构：

```json
{ "code": 200, "message": "success", "data": {} }
```

## 九、缓存

缓存实现：Spring Cache + Caffeine，TTL 5 分钟，最大 2000 条。

| 缓存名 | key 示例 | 失效时机 |
| --- | --- | --- |
| `videoPage` | `page=1&size=9&keyword=&category=全部&sortBy=create_time&order=desc` | 上传、删除、点赞、换封面 |
| `videoDetail` | `41` | 删除、点赞、播放、换封面 |
| `videoCategories` | `all` | 上传、删除 |
| `videoHot` | `6` | 上传、删除、点赞、播放、换封面 |

播放是高频写操作，只失效 `videoHot` 和 `videoDetail`，避免频繁清空分页缓存。缓存命中情况会输出到日志：

```text
CACHE MISS name=videoPage key=page=1&size=5&keyword=abc&category=全部&sortBy=create_time&order=desc
CACHE PUT  name=videoPage key=page=1&size=5&keyword=abc&category=全部&sortBy=create_time&order=desc
CACHE HIT  name=videoPage key=page=1&size=5&keyword=abc&category=全部&sortBy=create_time&order=desc
```

## 十、日志

| 文件 | 内容 | 滚动策略 |
| --- | --- | --- |
| `logs/app.log` | 全量日志、请求汇总、SQL DEBUG | 20MB/天，保留 30 天 |
| `logs/error.log` | ERROR 级别系统异常 | 20MB/天，保留 60 天 |
| `logs/business.log` | 业务异常与业务事件 | 20MB/天，保留 30 天 |
| `logs/security.log` | 注册、登录、401、403、限流 | 20MB/天，保留 60 天 |

链路追踪：

- 每个请求生成或复用 `X-Request-Id`，写入 MDC 的 `traceId`；
- 响应头返回 `X-Request-Id` 和 `X-Response-Time`；
- 登录成功后写入 `userId`，同一次请求的 Controller、Service、Mapper、缓存日志带同一个 `trace=`；
- 业务异常记录到 `business.log`，鉴权与越权记录到 `security.log`，未捕获异常记录到 `error.log`。

## 十一、鉴权与权限

- 密码使用 BCrypt（cost 12）摘要存储。
- 登录成功后签发 HS256 JWT，包含用户 ID、用户名、昵称、角色、签发时间和过期时间。
- `AuthInterceptor` 拦截 `/api/**`：登录、注册、分页、详情、分类、热门、流播放、封面读取、播放计数公开；上传、换封面、删除、点赞、`/api/auth/me` 需要登录。
- 上传时写入 `owner_id`；删除和换封面校验归属，普通用户只能操作自己的视频，管理员可以操作全部视频，越权返回 403。
- 登录连续失败 5 次后限流 10 分钟；拦截器会定期校验用户状态，禁用用户的 token 会尽快失效。

## 十二、视频封面

- 上传时可以选择 JPG、PNG、WebP 图片作为封面；不选择时由前端读取视频首帧并生成 16:9 JPEG。
- 封面单独存放在 `video.cover-path`，使用 UUID 重命名，通过文件头识别真实图片类型，单文件不超过 5MB。
- 数据库保存 `cover_file_name` 和 `cover_url`。
- `GET /api/video/cover/{fileName}` 公开读取，并设置浏览器缓存。
- 视频所有者或管理员可以在播放弹窗中更换封面；封面加载失败时前端回退为标题首字母占位图。
- 演示数据使用 `src/main/resources/static/covers/demo-*.svg` 作为默认封面。

## 十三、测试与验收

- 后端单元测试：`.\mvnw.cmd test`，共 21 个测试，覆盖 Range 播放、缓存 key、JWT、登录限流、鉴权与归属校验、视频元数据解析。
- 前端构建：`npm run build`。
- 一键验收：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scriptserify.ps1
```

脚本会自动完成注册、登录限流、缓存耗时、上传、封面读取、Range 播放、点赞去重、越权校验、删除和缓存失效等 28 项检查，并在结束后清理测试数据。

## 十四、常见问题

**1. 后端启动报 Port 8080 was already in use**

说明已经有一个后端实例在运行，先停止占用 8080 的进程，或通过 `SERVER_PORT` 更换端口。

**2. MySQL 连接失败**

检查 MySQL 服务、`DB_PASSWORD` 环境变量以及 `video_db` 是否存在。

**3. MyBatis 报 Invalid bound statement**

检查 `mapper-locations`、XML 的 `namespace` 和 statement id 是否与 Mapper 接口一致，检查 `target/classes/mapper` 下是否生成了 XML。

**4. 封面不显示**

确认 `uploads/covers/` 有写权限；演示封面由 Spring Boot 静态资源提供，路径为 `/covers/demo-*.svg`。

**5. 中文日志在 PowerShell 中乱码**

日志文件本身是 UTF-8，可在启动前执行 `chcp 65001`，或直接查看 `logs/app.log`。
