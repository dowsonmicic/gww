# 一期A步展示官网

Vue 3 + Nuxt 4 + TypeScript；默认中文，可切换英文。使用用户提供的 `.images/lb`、`show`、`brands` 图片副本，不连接数据库、Java、登录、OSS 或 AI。

## 本地运行

建议 Node 24 LTS。当前机器 Node 25 不在 Nuxt 最新版本支持范围，本次使用临时 Node 24.22.0 工具链验证。

安装锁定依赖：

```powershell
npm ci
```

启动开发服务（此命令不自动打开浏览器）：

```powershell
npm run dev
```

本地默认地址 `http://localhost:3000/zh`。不需要数据库或云凭证。

测试数据逻辑、类型、生产构建：

```powershell
npm test
npm run typecheck
npm run build
```

检查生产 HTTP 输出及默认语言，无预览窗口：

```powershell
npm run test:smoke
```

首次安装测试浏览器并运行无界面交互测试：

```powershell
npx playwright install chromium
npm run test:e2e
```

## 配置

以下环境变量在服务器启动时读取，改后重启即可，不必重建前端。不要把未确认公司信息或密钥填入公开配置。

| 变量 | 默认 | 用途 |
| --- | --- | --- |
| `SITE_DEFAULT_LOCALE` | `zh` | 只支持已启用的 `zh/en`；首次 `/` 跳转语言 |
| `SITE_COMPANY_NAME` | 材料展示 | 中文名称 |
| `SITE_COMPANY_NAME_EN` | Materials Showcase | 英文名称 |
| `SITE_CONTACT_EMAIL` | 空 | 已确认邮箱 |
| `SITE_CONTACT_PHONE` | 空 | 已确认电话 |
| `SITE_SUPPORT_URL` | 空 | HTTPS 人工客服外链，空时浮动入口跳联系页 |

显式 `/zh`、`/en` 优先于默认语言；手动切换保存在 `site-locale` Cookie，根路径优先尊重已选语言。语言 Cookie 非鉴权，错误值不会作为跳转路径。

例如先构建，再以英文默认启动生产服务：

```powershell
$env:SITE_DEFAULT_LOCALE="en"
npm start
```

## 内容与扩展

- `app/data/catalog.ts` 集中维护示例资料及本地查询，B步可替换为 API。
- `app/pages/[locale]/[[...slug]].vue` 提供首页及全部内容页面。
- 8400 等型号名称来自文件名；POE/DOW 是演示分类，不是已审核技术规格，正式发布前须核实。
- 品牌图不等于代理授权，未填写电话、证书、性能参数或供货承诺。
- 联系页无假表单；PDF/视频未提供，不展示虚假播放或下载按钮。
- 当前测试版设置 `noindex, nofollow`，不生成站点地图；这不是安全访问控制。
- 仅支持两种已完成翻译的语言，扩展语言要同步词条、内容、路由与配置校验。

## 部署与限制

阿里云已有 2核2GB 部署见 [部署说明](../一期测试方案/一期A步/一期A步_部署与验证说明.md)。构建在本地进行；不要在 2GB 服务器安装构建工具链或把源码构建当运行步骤。

HTTP/浏览器验证不代表阿里云负载、24小时运行或越南网络验收已完成。现有服务器尚未连接或修改。

依赖审计仍有上游 `braces`、`node-forge` 及相关构建/开发间接依赖告警，详情见验证说明；不能宣称零漏洞。正式上线前重新审计并跟踪上游修复，不使用强制降级解决。
