# 文档目录

| 目录/文件 | 用途 |
| --- | --- |
| [接口总索引](JAVA_API_INDEX.md) | 全部公开、共用与管理接口 |
| api/md、api/json | 公开接口和共用CSRF接口，分别为Markdown和OpenAPI JSON |
| admin/md、admin/json | 管理、登录、会话及退出接口 |
| [后台前端说明](admin/ADMIN_README.md) | /admin原型及媒体接入边界 |
| [Java后台说明](admin/JAVA_BACKEND_README.md) | 后端配置、存储、安全和运行 |
| [一期方案](plan/一期测试方案/一期总方案.md) | 官网、后台、Java/MySQL、本地媒体及上线验收 |
| [二期方案](plan/二期方案/二期总方案.md) | OSS、Redis及其他扩展 |
| [需求分析](plan/需求分析.md) | 业务需求与调研 |
| [技术方案](plan/技术方案.md) | 技术路线及扩展方向 |
| [阿里云测试方案](plan/阿里云已有资源测试方案.md) | 已有服务器测试与越南验收 |
| [外贸与AI参考](plan/外贸与AI客服补充方案.md) | 外贸访问及未来AI候选 |
| [Git说明](plan/GitREADME.md) | 仓库与分支操作 |

接口按实际路径分类：`/api/public/**`和`/api/auth/csrf`归api，其余管理与认证接口归admin。
共用CSRF接口也供管理员登录使用，此目录分类不改变接口权限。
在backend目录执行`Export-ApiDocs.ps1`会按上述规则生成接口文件及总索引。
源码项目README和SQL字段说明仍保留在website、backend及backend/sql中。
