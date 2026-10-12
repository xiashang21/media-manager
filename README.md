# VideoHub · media-manager

一个前后端分离的视频播放网站，包含视频上传、播放、分页检索、分类筛选、动态排序、点赞、逻辑删除、热门推荐和分类动态加载。

- 后端：Spring Boot 4 + 原生 MyBatis + MySQL 8（Maven 工程名：`video-backend`）
- 前端：Vue 3 + Vite + Axios（目录名：`video-frontend`）
- 后端端口：`8080`
- 前端端口：`5173`
- 数据库：`video_db`
- 上传目录：`./uploads/`


## 一、功能清单

- 视频上传：扩展名白名单、UUID 重命名、自动创建目录、上传进度条。
- 视频列表：展示标题、简介、分类、标签、大小、格式、时长、播放量、点赞数、上传者、上传时间。
- 视频播放：支持在线播放、拖动进度条、HTTP Range、断点续播。
- 视频检索：标题/简介关键字搜索、分类筛选、多字段排序。
- 动态排序：最新上传、播放最多、点赞最多、文件最大，支持升降序。
- 视频互动：播放量 +1、点赞 +1。
- 视频删除：数据库逻辑删除，并同步删除磁盘文件。
- 热门推荐：按播放量展示 Top N。
- 分类展示：分类标签和分类下拉均从数据库动态获取。
- 时长解析：后端解析 MP4/MOV、WebM/MKV、AVI 基础元数据，前端上传前读取时长作为兜底。

## 二、环境要求

- JDK 21+
- Maven 3.9+（项目自带 `mvnw.cmd`）
- Node.js 22.18+ 或 24.12+
- npm 10+
- MySQL 8.x

## 三、项目结构

```text
media-manager/
├─ src/main/java/pre/wc/mediamanager/
│  ├─ MediaManagerApplication.java     启动类
│  ├─ controller/                      接口层
│  ├─ service/  service/impl/          业务层
│  ├─ mapper/                          MyBatis Mapper
│  ├─ entity/  entity/dto/             视频实体和 DTO
│  ├─ pojo/                            原有媒体实体和 DTO
│  ├─ common/                          Result、PageResult、异常
│  ├─ config/                          CORS、全局异常、配置类
│  └─ util/                            视频时长解析工具
├─ src/main/resources/
│  ├─ application.yml                  数据库、MyBatis、上传目录配置
│  └─ mapper/VideoMapper.xml           MyBatis XML
├─ video-frontend/                              Vue 3 前端
├─ schema.sql                          建库建表
├─ data.sql                            30 条演示数据
└─ README.md
```

## 四、数据库初始化

先启动 MySQL，然后在项目根目录执行：

```powershell
Get-Content -Raw schema.sql | mysql -u root -p
Get-Content -Raw data.sql | mysql -u root -p
```

说明：

- `schema.sql` 创建 `video_db` 和 `video` 表。
- `data.sql` 写入 30 条演示数据。
- `data.sql` 使用 `REPLACE INTO`，可以重复执行；会更新已有演示数据。
- 演示数据的 `url` 指向公开测试视频地址，用于无本地视频时直接演示播放。
- 真实上传的视频会保存到 `./uploads/`，播放地址为 `/api/video/stream/{fileName}`。

## 五、配置说明

配置文件：`src/main/resources/application.yml`

默认配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/video_db?...
    username: root
    password: AHxs5702

server:
  port: 8080

mybatis:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true

video:
  upload-path: ./uploads/
```

支持环境变量覆盖：

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "video_db"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的密码"
$env:VIDEO_UPLOAD_PATH = "./uploads/"
```

上传目录默认是项目根目录下的 `uploads/`，不存在时会自动创建。

## 六、启动后端

在项目根目录执行：

```powershell
cd C:\Users\xiashang\Desktop\java\media-manager
.\mvnw.cmd spring-boot:run
```

启动成功后：

```text
Tomcat started on port 8080
Started MediaManagerApplication
```

如果端口 8080 被占用：

```powershell
netstat -ano | Select-String ':8080'
Stop-Process -Id <PID> -Force
```

停止服务：在运行窗口按 `Ctrl + C`。

运行后端测试：

```powershell
.\mvnw.cmd test
```

## 七、启动前端

```powershell
cd C:\Users\xiashang\Desktop\java\media-manager\video-frontend
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

前端使用 `VITE_API_BASE_URL` 指定后端地址：

```powershell
$env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev
```

前端构建：

```powershell
npm run build
```

## 八、接口列表

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | `/api/video/upload` | 上传视频，multipart 参数：file、title、description、category、tags、uploader、duration |
| GET | `/api/video/page` | 分页查询，参数：page、size、keyword、category、sortBy、order |
| GET | `/api/video/{id}` | 视频详情 |
| DELETE | `/api/video/{id}` | 逻辑删除并同步删除磁盘文件 |
| POST | `/api/video/{id}/play` | 播放量 +1 |
| POST | `/api/video/{id}/like` | 点赞数 +1 |
| GET | `/api/video/categories` | 分类列表 |
| GET | `/api/video/hot?limit=6` | 热门 Top N |
| GET | `/api/video/stream/{fileName}` | 在线播放，支持 HTTP Range 和断点续播 |

统一返回结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

保留的原有实训接口：

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/media/config` | 读取自定义配置 |
| GET | `/media/list` | 演示媒体列表 |
| GET | `/api/v1/info` | 读取 app.name、app.version |

## 九、Range 播放说明

`GET /api/video/stream/{fileName}` 支持以下请求：

- 无 Range：返回前 1MB，状态码 `206 Partial Content`。
- `Range: bytes=start-end`：返回 `206 Partial Content`，单次最多 1MB。
- `Range: bytes=start-`：end 取 min(start + 1MB - 1, fileLength - 1)。
- 使用 ResponseEntity<ResourceRegion> 实现，分块大小 1MB。
- 非法范围：返回 `416 Requested Range Not Satisfiable`。

响应头包含：

```text
Accept-Ranges: bytes
Content-Range: bytes start-end/total
Content-Length: 当前片段长度
Content-Type: video/mp4
```

实现采用 ResourceRegion 分块流式写出，不会把整个视频文件一次性读入内存。

## 十、前端设计

前端目录：`video-frontend/`

当前界面风格：

- 白蓝简洁视频站风格
- 顶部搜索、排序、上传入口
- 分类标签导航
- 热门推荐 Banner
- 视频卡片网格
- 右侧热门播放榜
- 上传抽屉
- 播放弹窗
- Toast 提示
- 响应式布局，支持移动端

## 十一、常见问题

**1. 演示数据播放不了**

先确认网络可以访问 `data.sql` 中的视频地址；如果网络受限，推荐直接使用页面上传本地视频，上传后的视频走本项目自己的 Range 播放接口，不依赖外部网络。

**2. 上传后不显示**

检查 `uploads/` 目录是否有写权限，并查看后端控制台是否有数据库异常。

**3. MyBatis 报 Invalid bound statement**

检查：

- `target/classes/mapper/VideoMapper.xml` 是否存在
- `mapper-locations` 是否为 `classpath:mapper/*.xml`
- XML 的 `namespace` 是否为 `pre.wc.mediamanager.mapper.VideoMapper`
- XML 的 `id` 是否和 Mapper 方法名一致

**4. 前端请求跨域失败**

确认后端 `WebConfig` 允许 `http://localhost:5173`，并暴露 `Content-Range`、`Accept-Ranges`、`Content-Length`。



