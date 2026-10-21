# Java 官网后台

## 功能与结构

采用 Java 17、Spring Boot、MyBatis、MySQL、Spring Security 和 Knife4j。Redis暂时禁用，
会话、缓存和限流保存在本机内存，重启会清空，不支持多实例共享。
支持管理员登录、内容管理、客户询盘及服务器媒体上传与发布。

```text
backend/
  pom.xml                 父工程，统一依赖版本
  server/                 Java 服务
    src/main/java/        认证、内容、媒体、询盘模块
    src/main/resources/   分环境配置、Flyway 数据库迁移
    src/test/             H2、本机会话、临时文件测试
  .env.example            配置模板
  .env                    dev本机配置，被Git忽略
  sql/                    MySQL手动初始化SQL及后续扩展模板
  nginx-api.conf.example  API 与公开媒体的 Nginx 转发模板
  Export-ApiDocs.ps1       接口文档生成脚本
```

公开及共用接口放`docs/api/md/`与`docs/api/json/`；
管理/认证接口放`docs/admin/md/`与`docs/admin/json/`。
统一索引为`docs/JAVA_API_INDEX.md`，方案和Git说明放`docs/plan/`。

当前官网及 `/admin` 尚未接入 Java API，仍为前端演示数据。

## 直接运行本机版本

`backend/.env`当前已改为本机MySQL配置，Redis仍禁用。
已只读验证MySQL 8.0.38的gww库，三张业务表及索引与初始化结构一致，暂无数据。
由于已手动导入表且没有Flyway历史记录，本次运行临时禁用自动迁移，不重复执行V1：

```powershell
java -Xms128m -Xmx384m -jar server/target/server-1.0.0.jar --spring.profiles.active=dev --spring.flyway.enabled=false
```

服务地址为`http://127.0.0.1:8080`，管理员账号为admin，密码读取本地.env。
当前数据库位于MySQL，旧H2文件保留在`backend/local-media/database/`，未迁移或删除。
私有与公开媒体分别位于
`backend/local-media/private/`与`backend/local-media/public/`。这些目录被Git忽略，
不在target、JAR或.output内；云服务器部署时须改为核实后的独立绝对路径。
连接MySQL不要启用local，local会强制使用H2驱动。
恢复Flyway前先备份并核对已有结构，再明确建立版本1基线；不要对未知库自动baseline。
禁用Flyway期间不会自动执行V2等新增迁移，此配置仅用于当前既有表的连接验证。
公开/media/仍需Nginx映射，Java不直接提供公开静态文件路由。

本次MySQL验证：真实连接、公开产品查询、HTTP登录及后台产品列表成功；未进行写入验收。
此前12项H2自动化测试通过，不等同MySQL迁移或生产容量验收。

## A步：准备运行环境

准备 JDK 17、Maven 3.9、MySQL 8，不需要启动 Redis。创建独立的 `gww` 数据库，
不要用现有业务库试运行，首次启动由 Flyway 创建表。

按照 [.env.example](.env.example) 在终端或进程管理器中注入配置：

| 变量 | 说明 |
| --- | --- |
| DB_URL | MySQL 连接地址，默认本机 gww 数据库 |
| DB_USERNAME / DB_PASSWORD | 数据库账号与密码 |
| ADMIN_PASSWORD | dev普通密码，启动时转为BCrypt，不写入数据库 |
| ADMIN_USERNAME | 管理员账号，默认 admin |
| ADMIN_PASSWORD_HASH | BCrypt哈希，优先于普通密码；prod必填 |
| SPRING_PROFILES_ACTIVE | 本机 HTTP 使用 dev，生产 HTTPS 使用 prod |
| SERVER_ADDRESS / SERVER_PORT | 默认 127.0.0.1:8080 |

`.env.example` 仅作模板；dev从backend或server目录启动时会读取backend/.env。
生产环境不自动读取此文件，请注入环境变量。不要在HASH字段填普通密码。
不要提交密码、哈希或实际 `.env` 文件，也不要将密码写入日志。

## B步：预留服务器文件目录

云服务器及宝塔实际目录尚待核实，不写死部署路径。

- `MEDIA_PRIVATE_ROOT`：私有上传目录的绝对路径。
- `MEDIA_PUBLIC_ROOT`：已发布副本目录的绝对路径。
- `MEDIA_PUBLIC_URL_PREFIX`：默认 `/media/`。
- `MEDIA_MAX_UPLOAD_BYTES`：默认 20971520，即 20MB。

两目录必须独立，不能互相包含。禁止放入 Nuxt `.output` 或 JAR 部署目录。
Nginx 只映射公开目录，私有下载必须经过 Java 管理员认证。
未配置目录时媒体操作返回 503；媒体配置留空不会自动选择生产目录。
数据库记录媒体标识及元数据，不向浏览器暴露绝对磁盘路径。

## C步：测试、构建与启动

以下命令在 `D:\work\gww\backend` 执行，不会自动打开浏览器。

运行测试并构建 JAR：

```powershell
mvn -B clean package
```

测试使用 H2、本机会话和临时目录，不需要生产数据库凭证。
测试通过并不代表真实 MySQL或云服务器已验证。

完成 A步配置后启动服务：

```powershell
java -Xms128m -Xmx384m -jar server/target/server-1.0.0.jar
```

服务地址：`http://127.0.0.1:8080`。
公开内容接口：`GET /api/public/content/products`。
开发环境 Knife4j 地址：`/doc.html`，需要先登录；生产默认关闭在线文档。
后端没有官网首页，直接访问 `/` 未登录时返回 401 属于正常权限控制。
前台 `/admin` 属于独立的 Nuxt 服务，不是 Java 8080 端口提供的网页。

登录前 GET `/api/auth/csrf` 并保留会话 Cookie，登录 POST 请求携带返回的
CSRF 请求头及令牌。登录后重新获取令牌，后续写请求保持同一 Cookie。
运行中的 Java 服务可在所属终端按 Ctrl+C 停止。

若启动失败，优先检查数据库是否存在、账号密码是否正确、
管理员密码或哈希是否填写及 8080 是否被占用。不要通过关闭认证解决启动失败。

## D步：文档与部署

- [完整后台与部署说明](../docs/admin/JAVA_BACKEND_README.md)
- [每个接口的 Markdown 与 OpenAPI JSON 索引](../docs/JAVA_API_INDEX.md)
- [Nginx 合并配置模板](nginx-api.conf.example)
- [SQL初始化及后续迁移说明](sql/README.md)

现有测试在本机 JDK 21 下按 `release 17` 编译执行，真实 JDK 17 运行仍待验证。
未部署到云服务器，尚未完成真实 MySQL/Flyway及前后端联调。
发布前还需完善可信代理限流、验证码和文件审核，详见完整说明。

## 早期运行结果（禁用Redis之前，2026-10-20）

- 执行 `mvn -B test`：10 项测试通过，失败和错误均为 0。
- 执行 JAR 启动：因缺少 `ADMIN_PASSWORD_HASH` 而退出，服务未保持运行。
- 本机 MySQL 3306 端口在监听，但数据库密码未注入，连接和建表尚未验证。
- 本机 Redis 6379 端口未监听，需启动 Redis 并配置连接参数。
- 未猜测密码、未修改现有数据库、未连接云服务器。

## 禁用Redis后的验证

- Redis依赖已注释，使用本机会话、内存缓存和本机限流。
- `mvn -B clean package`：12项测试通过。
- dev可读取本地`.env`的`ADMIN_PASSWORD`，无需手工生成哈希；prod仍要求BCrypt。
- JAR启动已通过管理员配置检查，当前因未配置`DB_PASSWORD`被MySQL拒绝，
  服务已退出。数据库密码与管理员登录密码互相独立。
