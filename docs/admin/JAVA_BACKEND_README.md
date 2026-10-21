# Java官网管理后台

## 当前实现

Java 17目标，Spring Boot 3.5.16、MyBatis、MySQL、Flyway、Spring Security、BCrypt、Knife4j/SpringDoc。
Redis及Spring Session依赖暂时注释禁用，改用Servlet Session、本机缓存和固定窗口限流。
重启会清空会话、缓存和限流，多实例部署需恢复共享实现。

- 配置式单管理员登录、会话查询、退出、CSRF保护和登录限流。
- 产品、分类、品牌、文章、网站设置的通用内容CRUD，提供公开已发布内容接口。
- 图片/PDF/视频上传、私有下载、显式公开发布。
- 客户询盘提交，管理员完整联系方式列表、单条详情、日期范围Excel导出和跟进状态修改；按提交时间倒序。
- 数据库初始迁移不插入演示内容。不会修改当前官网的catalog.ts。

**当前官网及/admin仍使用本地演示数据，尚未接入Java接口。** 不包含完整ERP、用户角色管理、验证码、媒体删除/撤销发布或OSS实现。内容目前为中英文固定字段，新增语言需要扩展数据模型。

## A步：配置环境

使用JDK17、Maven3.9、MySQL8，暂不需要Redis。创建独立的gww数据库，使用最小权限数据库账号；Flyway首次启动建表。不要连接现有业务库试运行。

配置项见 `backend/.env.example`。dev从backend或server目录启动时读取被Git忽略的backend/.env；
生产配置由Shell、宝塔Java运行环境或进程管理器注入，不自动读取开发密码文件。

- `DB_PASSWORD`、`REDIS_PASSWORD`及`ADMIN_PASSWORD_HASH`不得提交到Git。
- dev可配置ADMIN_PASSWORD普通密码，启动时在内存中转为BCrypt，不写入数据库。ADMIN_PASSWORD_HASH仍只接受BCrypt哈希并优先使用，prod必须提供哈希；不要把密码提交Git。
- 生产使用`SPRING_PROFILES_ACTIVE=prod`和HTTPS，Cookie Secure=true；本机HTTP调试使用dev。
- 默认仅监听127.0.0.1:8080，不向公网开放MySQL/Redis/Java端口。

## B步：预留云服务器媒体位置

实际宝塔站点路径尚未核实，因此不假定任何云服务器目录。

| 变量 | 含义 |
| --- | --- |
| MEDIA_PRIVATE_ROOT | 独立私有绝对目录，保存原始上传及草稿 |
| MEDIA_PUBLIC_ROOT | 独立公开绝对目录，只存显式发布的副本 |
| MEDIA_PUBLIC_URL_PREFIX | 浏览器访问前缀，默认/media/ |
| MEDIA_MAX_UPLOAD_BYTES | 单文件字节上限，默认20971520 |

两目录不能互相包含，不能位于`.output`，也不要放到JAR部署目录。目录未配置时媒体接口返回503，其他功能仍可使用。数据库只保存随机object_key和元数据，不返回服务器绝对路径。

Java运行账号拥有两目录写权限；Nginx仅拥有公开目录读权限。禁止映射私有目录、开启目录索引或执行上传文件。Docker部署必须挂载这两个持久目录，不能依赖容器可写层。

支持JPEG、PNG、WebP、PDF、MP4，检查后缀、文件头和大小，随机命名并阻止路径穿越。**文件头检查不等于病毒扫描或完整格式验证**；PDF等文件正式公开前需要人工审核或恶意文件扫描，宜配置独立媒体域名降低同源内容风险。

上传/发布失败时进行文件补偿；进程突然中断或断电仍可能遗留孤儿文件，需定期对照数据库检查。备份MySQL与两个媒体目录，保留时间点一致的快照，并验证恢复。当前无媒体删除接口。

## C步：构建与运行

以下命令在`backend`目录执行，不自动打开浏览器。

运行测试并构建可执行JAR：

```powershell
mvn -B clean package
```

完成环境变量注入后启动；以下内存值仅为2GB服务器的初始测试值，需结合Nuxt/MySQL实际占用调整。Redis、OSS均属二期：

```sh
java -Xms128m -Xmx384m -XX:MaxMetaspaceSize=192m -jar server/target/server-1.0.0.jar
```

JVM还占用堆外、线程栈等内存，Xmx不等于进程总占用。使用宝塔或systemd守护进程，不以root长期运行Java。没有部署到你的云服务器。

Nginx同源API及公开媒体转发模板见`backend/nginx-api.conf.example`，先核实已有server块与实际目录，再合并配置，不要直接覆盖。测试期Knife4j `/doc.html`需要管理员登录，生产环境默认关闭在线文档。

当前限流根据连接IP计算，同机Nginx代理时会共用127.0.0.1额度。多访客上线前须配置受信代理IP解析并验证本机限流，不能直接信任任意X-Forwarded-For。Redis共享原子限流放入二期，不作为一期启动条件。当前没有验证码，正式询盘上线前需补充防机器人措施。

## D步：接口与验证

接口文档见[JAVA_API_INDEX](../JAVA_API_INDEX.md)。公开及共用接口位于`docs/api/md/`和`docs/api/json/`，管理/认证接口位于`docs/admin/md/`和`docs/admin/json/`，索引位于`docs/JAVA_API_INDEX.md`。以上路径均相对于项目根目录。

登录流程：GET `/api/auth/csrf`，保留Cookie，POST `/api/auth/login`携带返回的请求头；登录后再次获取令牌。所有写请求均携带最新令牌及同一Cookie。

更新实际接口定义并重生成文档，依次执行：

```powershell
mvn -B test "-Dopenapi.export=$env:TEMP/gww-openapi.json"
.\Export-ApiDocs.ps1
```

测试使用H2、本机会话和临时文件目录。询盘专项测试验证管理员权限、完整联系方式、详情、排序、日期全天边界、超过100条和空结果Excel导出、公式文本安全。当前本机JDK21按release17编译运行测试；本次未执行真实MySQL联调或全量测试。H2测试不能替代MySQL/Flyway迁移，也不代表2GB云服务器容量与越南访问验收通过。

当前列表限制：内容管理返回分页数组但无total，公开内容最多100条；媒体最多100条，询盘列表及导出不限制100条。询盘导出使用MySQL服务器游标逐批读取并流式生成xlsx；dev/prod的Hikari配置启用useCursorFetch/useServerPrepStmts。日期使用数据库服务器时间，不由浏览器时区换算。
