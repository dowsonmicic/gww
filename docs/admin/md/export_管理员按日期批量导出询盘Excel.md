# 管理员按日期批量导出询盘Excel

`
GET
 
/api/admin/inquiries/export
`

请求与响应结构以同名OpenAPI JSON为准，可导入Apifox或其他OpenAPI工具。

## 调用约定

- 管理接口及me/logout必须先登录，携带GWWSESSION Cookie。
- 登录、退出、询盘及其他写请求都需先GET /api/auth/csrf，然后携带返回的headerName/token及同一会话Cookie。
- 登录后重新获取CSRF令牌。不要把Cookie或令牌写入日志。
- 普通JSON响应为code/msg/data，code=0表示成功；文件下载直接返回二进制。
- 常见HTTP状态：400参数错误、401未登录、403权限或CSRF错误、404不存在、429限流、500服务异常。

## 参数

- startDate：位置=query，必填=False，结构见JSON。
- endDate：位置=query，必填=False，结构见JSON。
- locale：位置=query，必填=False，结构见JSON。

## 业务约束

- kind仅支持products/categories/brands/articles/settings。
- articles使用单一title（必填），不使用name/nameEn；其他内容name/nameEn必填。文章响应只返回title，不返回name/nameEn。
- description为内容，descriptionEn为英文内容，均选填；文章两种语言共用标题。
- 内容状态DRAFT/PUBLISHED/ARCHIVED；公共接口不返回草稿，列表最多100条。
- imageId只能引用已发布媒体。内容说明为纯文本，不应直接用于v-html。
- 上传默认最大20MB，支持JPEG/PNG/WebP/PDF/MP4。先存私有目录，显式发布后才生成/media/地址。
- 询盘状态NEW/PROCESSING/CLOSED；仅管理员可读取完整电话/邮箱，列表和详情含createdAt，按时间倒序。
- GET /api/admin/inquiries/export输出真实xlsx；startDate/endDate为yyyy-MM-dd且包含起止日全天（数据库服务器时间）；locale可选zh/en，不指定条件导出全部，超过100条不截断。
- 询盘姓名、邮箱、电话、留言和locale必填；国家、行业、产品、HTTP/HTTPS网站及公司选填。后台按locale分区。
- 网站基础设置保存至MySQL，公开接口仅返回企业中英文信息、联系方式、默认语言及HTTPS客服URL，无敏感配置。
- 内容管理列表使用page/size，当前返回数组，不包含total；媒体最多100条，询盘列表及导出不限制100条。