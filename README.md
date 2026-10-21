# 材料展示外贸网站

参考企业材料展示网站建设的中英文官网，优先面向越南市场。一期包含官网、Java/MySQL、/admin管理后台、服务器本地媒体和询盘；OSS、Redis及其他增强项移入二期。

## 当前功能

- 首页轮播：使用 `.images/lb` 提供的图片。
- 产品列表、分类/品牌筛选、型号搜索、分页及产品详情。
- 品牌展示：使用 `.images/brands` 提供的图片。
- 关于我们、新闻资讯和联系我们；材料知识与荣誉资质栏目已移除，现有文章统一在新闻资讯展示。
- 默认中文，支持中英文切换和语言选择记忆。
- 手机导航、响应式布局、404 页面。
- 单一联系入口；未配置人工客服渠道时跳转联系页。

当前官网产品/文章与 `/admin` 产品、文章、询盘已接入Java/MySQL，不再从静态数组填充；数据库为空时显示空内容。产品图片支持上传并关联，分类与品牌只读汇总。轮播/合作商标替换和网站设置仍为页面预览。详见 [后台说明](docs/admin/ADMIN_README.md)。

运行前端前需启动MySQL和Java；Nuxt同源代理默认连接 `http://127.0.0.1:8080`。已通过本地产品新增、SQL落库、刷新、双语SSR及图片上传/权限联调；尚未部署云服务器。详见 [后端运行说明](backend/README.md)和[接口文档索引](docs/JAVA_API_INDEX.md)。

企业名称、联系方式、技术参数及资质尚待确认。产品资料由管理员录入并发布，品牌图片不代表代理或合作授权。

## 技术栈

| 项目 | 当前版本或安排 |
| --- | --- |
| 运行环境 | 推荐 Node.js 24 LTS |
| 官网框架 | Nuxt 4.6.2、Vue 3.5.44 |
| 开发语言 | TypeScript 5.9.3 |
| 路由 | Vue Router 5.4.0 |
| 测试 | Node Test Runner、Playwright |
| 部署 | Nginx、Docker Compose |
| 一期后台 | 同一Nuxt工程的/admin，Vue、Element Plus |
| 一期后端 | Java 17、Spring Boot、MyBatis、MySQL；H2仅本地调试 |
| 一期存储与会话 | 服务器独立磁盘目录、Servlet Session、本机缓存及限流 |
| 接口文档 | Knife4j、SpringDoc/OpenAPI |
| 二期扩展 | Redis、OSS、按需CDN、多角色、多语言及AI评估 |

Element Plus用于/admin，官网采用定制样式。当前机器Node 25不在Nuxt版本支持范围内，建议使用Node 24 LTS。

## 快速启动

在终端将工作目录设置为 `D:\work\gww\website`。以下 npm 命令均在该目录执行。

安装锁定依赖：

```powershell
npm ci
```

启动本地开发服务，不自动打开浏览器：

```powershell
npm run dev
```

默认访问地址：

- 中文：`http://localhost:3000/zh`
- 英文：`http://localhost:3000/en`
- 根路径：`http://localhost:3000/`，按语言记忆或默认配置跳转。

生成生产构建并启动：

```powershell
npm run build
npm start
```

## 环境变量

配置在服务启动时读取，修改后重启生效，不需要重建前端。以下信息会展示给访客，不能填入密钥或内部资料。

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SITE_DEFAULT_LOCALE` | `zh` | 默认语言，仅支持 `zh` 或 `en` |
| `SITE_COMPANY_NAME` | 材料展示 | 中文企业名称 |
| `SITE_COMPANY_NAME_EN` | Materials Showcase | 英文企业名称 |
| `SITE_CONTACT_EMAIL` | 空 | 联系邮箱 |
| `SITE_CONTACT_PHONE` | 空 | 联系电话 |
| `SITE_SUPPORT_URL` | 空 | HTTPS 人工客服外链，空时跳转联系页 |

例如将默认语言改为英文后启动：

```powershell
$env:SITE_DEFAULT_LOCALE = "en"
npm start
```

显式访问 `/zh`、`/en` 时以 URL 为准；根路径优先尊重用户已保存的语言选择。`site-locale` Cookie 有效期一年，不用于登录认证。

直接运行 npm 时使用系统环境变量；Docker Compose 可通过部署目录 `.env` 注入。不要假定生产 `npm start` 会自动读取 `.env`。

## 项目结构

```text
gww/
├── .images/                    原始图片素材
│   ├── lb/                     轮播图
│   ├── show/                   产品图
│   └── brands/                 品牌图
├── website/                    当前官网项目
│   ├── app/
│   │   ├── app.vue             导航、页脚、语言切换及联系入口
│   │   ├── assets/             官网样式
│   │   ├── components/         轮播与产品卡片
│   │   ├── data/catalog.ts     双语词条、展示数据及查询逻辑
│   │   └── pages/              中英文页面与路由
│   ├── server/                 Nuxt 配置注入，不是 Java 业务后端
│   ├── public/images/          实际供页面使用的素材副本
│   ├── tests/                  数据、HTTP 及浏览器测试
│   ├── deploy/                 Nginx 配置示例
│   ├── Dockerfile
│   ├── compose.yml
│   └── README.md               前端详细说明
└── docs/                       分类文档
    ├── api/                   公开及共用接口md/json
    ├── admin/                 管理接口md/json及后台说明
    ├── plan/                  需求、技术、Git、一期与二期方案
    └── JAVA_API_INDEX.md       接口总索引
```

修改原始 `.images` 后，需要同步对应的 `website/public/images` 文件并重新构建；页面不会直接读取原始素材目录。

## 测试命令

执行数据测试和类型检查：

```powershell
npm test
npm run typecheck
```

生产构建后验证 HTTP 页面、404、素材和语言配置：

```powershell
npm run build
npm run test:smoke
```

首次准备测试浏览器，再运行无界面交互测试：

```powershell
npx playwright install chromium
npm run test:e2e
```

格式化源码：

```powershell
npm run format
```

当前本地构建、类型检查、3项数据测试、双语 HTTP 检查及5项浏览器测试已通过，包含375/768/1440宽度检查。未完成阿里云部署、24小时稳定性和越南网络验收。

## 分步计划

| 阶段 | 目标 |
| --- | --- |
| [一期A步](docs/plan/一期测试方案/一期A步/一期A步_展示网站方案.md) | 展示官网、本地数据及图片，已完成本地实现 |
| [一期B步](docs/plan/一期测试方案/一期B步/一期B步_公共接口与询盘方案.md) | Java 公共接口、数据库和真实询盘 |
| [一期C步](docs/plan/一期测试方案/一期C步/一期C步_登录后台与本地媒体方案.md) | 管理员登录、/admin真实API联调和服务器本地媒体 |
| [一期D步](docs/plan/一期测试方案/一期D步/一期D步_上线与越南验收方案.md) | 人工客服、上线及越南访问验收 |
| [二期方案](docs/plan/二期方案/二期总方案.md) | Redis、OSS、增强权限、多语言及AI评估，需另行批准 |

一期不启动Redis、不接OSS、不部署AI。后台和Java基础实现已存在但尚未联调；新增语言和增强权限属于二期，不是已存在的隐藏功能。

## 阿里云部署

复用已有2核2GB服务器。A步只运行 Nuxt 和 Nginx，在本地构建后上传产物；不在该服务器执行 npm/Maven 构建。

Compose 默认仅绑定 `127.0.0.1:3100`，由 Nginx 反向代理。部署前核实原有业务、资源、域名、HTTPS及备案条件，不覆盖现有站点。

详细步骤见 [A步部署与验证说明](docs/plan/一期测试方案/一期A步/一期A步_部署与验证说明.md)。当前尚未连接或修改服务器。

## 注意事项

- 测试版设置 `noindex, nofollow`，不生成站点地图；这不能替代访问控制。
- 不使用虚构联系方式、性能参数、证书或成功提交提示。
- 尚有上游构建/开发间接依赖安全告警，正式上线前重新审计并跟踪补丁，不宣称零漏洞。
- 现有服务器地域不默认新加坡；越南可达性需要实际跨运营商测试。二期OSS地域另行核实。

## 文档入口

- [分类文档总入口](docs/README.md)
- [前端详细说明](website/README.md)
- [一期总方案](docs/plan/一期测试方案/一期总方案.md)
- [二期扩展方案](docs/plan/二期方案/二期总方案.md)
- [需求分析](docs/plan/需求分析.md)
- [技术方案](docs/plan/技术方案.md)
- [阿里云已有资源测试方案](docs/plan/阿里云已有资源测试方案.md)
- [越南访问及未来 AI 参考](docs/plan/外贸与AI客服补充方案.md)
