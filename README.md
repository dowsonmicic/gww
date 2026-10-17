# 材料展示外贸网站

参考企业材料展示网站建设的中英文官网，优先面向越南市场。当前完成一期A步的本地展示页面，后续逐步接入 Java、数据库、管理后台及阿里云 OSS。

## 当前功能

- 首页轮播：使用 `.images/lb` 提供的图片。
- 产品列表、分类/品牌筛选、型号搜索、分页及产品详情。
- 品牌展示：使用 `.images/brands` 提供的图片。
- 关于我们、新闻资讯、材料知识、荣誉资质和联系我们。
- 默认中文，支持中英文切换和语言选择记忆。
- 手机导航、响应式布局、404 页面。
- 单一联系入口；未配置人工客服渠道时跳转联系页。

当前使用本地展示数据和项目静态图片，**不需要数据库、Java 服务或 OSS 凭证**。没有登录、后台、真实询盘提交或 AI 客服，前端不显示这些未实现功能。

企业名称、联系方式、技术参数及资质尚待确认。型号分类属于演示数据，品牌图片不代表代理或合作授权。

## 技术栈

| 项目 | 当前版本或安排 |
| --- | --- |
| 运行环境 | 推荐 Node.js 24 LTS |
| 官网框架 | Nuxt 4.6.2、Vue 3.5.44 |
| 开发语言 | TypeScript 5.9.3 |
| 路由 | Vue Router 5.4.0 |
| 测试 | Node Test Runner、Playwright |
| 部署 | Nginx、Docker Compose |
| 后续后台 | Vue、Element Plus |
| 后续后端 | Java 17、Spring Boot、MyBatis、MySQL、Redis |
| 后续接口文档 | Knife4j、SpringDoc/OpenAPI |

Element Plus 用于后续管理后台，当前官网采用定制样式。当前机器 Node 25 不在 Nuxt 版本支持范围内，建议使用 Node 24 LTS。

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
└── 一期测试方案/                分步实施及部署文档
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
| [一期A步](./一期测试方案/一期A步/一期A步_展示网站方案.md) | 展示官网、本地数据及图片，已完成本地实现 |
| [一期B步](./一期测试方案/一期B步/一期B步_公共接口与询盘方案.md) | Java 公共接口、数据库和真实询盘 |
| [一期C步](./一期测试方案/一期C步/一期C步_登录后台与OSS方案.md) | 管理员登录、Element Plus 后台和 OSS |
| [一期D步](./一期测试方案/一期D步/一期D步_上线与越南验收方案.md) | 人工客服、上线及越南访问验收 |

AI 仅保留未来方案，不属于当前一期部署范围；新增语言、登录和存储能力在后续阶段实现，不是当前已存在的隐藏功能。

## 阿里云部署

复用已有2核2GB服务器。A步只运行 Nuxt 和 Nginx，在本地构建后上传产物；不在该服务器执行 npm/Maven 构建。

Compose 默认仅绑定 `127.0.0.1:3100`，由 Nginx 反向代理。部署前核实原有业务、资源、域名、HTTPS及备案条件，不覆盖现有站点。

详细步骤见 [A步部署与验证说明](./一期测试方案/一期A步/一期A步_部署与验证说明.md)。当前尚未连接或修改服务器。

## 注意事项

- 测试版设置 `noindex, nofollow`，不生成站点地图；这不能替代访问控制。
- 不使用虚构联系方式、性能参数、证书或成功提交提示。
- 尚有上游构建/开发间接依赖安全告警，正式上线前重新审计并跟踪补丁，不宣称零漏洞。
- 现有服务器和 OSS 地域不默认新加坡；越南可达性需要实际跨运营商测试。

## 文档入口

- [前端详细说明](./website/README.md)
- [一期总方案](./一期测试方案/一期总方案.md)
- [需求分析](./需求分析.md)
- [技术方案](./技术方案.md)
- [阿里云已有资源测试方案](./阿里云已有资源测试方案.md)
- [越南访问及未来 AI 参考](./外贸与AI客服补充方案.md)
