# SQL 文件说明

## A步：选择初始化方式

- `gww_mysql_init.sql`：MySQL 8手动建库、建表文件，适用于全新独立数据库。
- `../server/src/main/resources/db/migration/V1__initial.sql`：应用Flyway自动建表。

推荐使用Flyway：先创建空的gww数据库并配置账号，启动应用完成建表。
手动SQL和Flyway二选一，不要先导入表再直接运行V1。
手动文件故意不使用DROP或CREATE TABLE IF NOT EXISTS，遇到已有表时应停止并检查，
不能将忽略重复表错误当作迁移成功。MySQL DDL可能部分执行，重复执行前先核查。

若已经手动导入且准备交给Flyway管理，备份并确认表结构与V1一致后，
才可建立版本1基线；不要对未知数据库自动启用baseline-on-migrate。
本文件不包含密码、管理员默认账号或客户数据。

## 数据库字段表

以下字段与`gww_mysql_init.sql`一致。数据库名称为`gww`，三张表均使用
InnoDB、`utf8mb4`字符集和`utf8mb4_unicode_ci`排序规则。
“不可空”指数据库的NOT NULL约束，不代表字符串必须非空；非空及长度校验由Java补充。
“无”表示SQL未声明默认值，插入时应由应用提供。

### 内容表：content_record

统一管理产品、分类、品牌、文章和网站设置。

| 字段 | 数据类型 | 可为空 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| id | VARCHAR(36) | 否 | 无 | 主键 | 内容UUID，由Java生成 |
| kind | VARCHAR(20) | 否 | 无 | 联合索引第1列 | 内容类型：products/categories/brands/articles/settings |
| name | VARCHAR(120) | 否 | 无 | 无 | 中文名称 |
| name_en | VARCHAR(180) | 否 | 无 | 无 | 英文名称 |
| category | VARCHAR(80) | 否 | 无 | 无 | 材料分类，保存字符串，未关联分类表外键 |
| brand | VARCHAR(80) | 否 | 无 | 无 | 品牌，保存字符串，未关联品牌表外键 |
| status | VARCHAR(20) | 否 | 无 | 联合索引第2列 | DRAFT草稿、PUBLISHED已发布、ARCHIVED已归档 |
| description | TEXT | 否 | 无 | 无 | 中文说明，按纯文本使用，接口当前最多20000字符 |
| description_en | TEXT | 否 | 无 | 无 | 英文说明，按纯文本使用，接口当前最多20000字符 |
| image_id | VARCHAR(36) | 是 | NULL | 无数据库外键 | 引用media_record.id，由服务层校验媒体存在且已发布 |
| updated_at | TIMESTAMP | 否 | CURRENT_TIMESTAMP | 无 | 插入时默认当前时间，内容修改时由Java SQL显式更新 |

索引：`PRIMARY KEY (id)`、`idx_content_kind_status (kind, status)`。
`updated_at`没有`ON UPDATE`声明，直接手动修改其他字段不会自动更新此时间。

### 媒体表：media_record

保存上传文件的标识及元数据，公开与私有目录由服务端环境变量配置。

| 字段 | 数据类型 | 可为空 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| id | VARCHAR(36) | 否 | 无 | 主键 | 媒体UUID，由Java生成 |
| object_key | VARCHAR(100) | 否 | 无 | 唯一索引 | 随机文件名，相对于媒体根目录，不是绝对磁盘路径 |
| original_name | VARCHAR(180) | 否 | 无 | 无 | 清理后的原始文件名，仅展示和下载命名使用 |
| content_type | VARCHAR(80) | 否 | 无 | 无 | 服务端依据允许的类型及文件头确定的MIME类型 |
| size_bytes | BIGINT | 否 | 无 | 无 | 文件大小，单位字节，上传上限由配置及Java控制 |
| published | BOOLEAN | 否 | FALSE | 无 | 是否公开发布：FALSE私有，TRUE已发布 |
| created_at | TIMESTAMP | 否 | CURRENT_TIMESTAMP | 无 | 上传记录创建时间 |

索引：`PRIMARY KEY (id)`、`uk_media_object_key (object_key)`唯一索引。
MySQL中的`BOOLEAN`为`TINYINT(1)`别名，当前未通过CHECK限制值为0或1。
Java文件头检查不等同于病毒扫描或完整格式验证。

### 询盘表：inquiry_record

保存客户联系信息及跟进状态。

| 字段 | 数据类型 | 可为空 | 默认值 | 约束/索引 | 说明 |
| --- | --- | --- | --- | --- | --- |
| id | VARCHAR(36) | 否 | 无 | 主键 | 询盘UUID，由Java生成 |
| name | VARCHAR(80) | 否 | 无 | 无 | 客户名称 |
| email | VARCHAR(180) | 否 | 无 | 无 | 联系邮箱，数据库保存原值，接口校验邮箱格式 |
| phone | VARCHAR(40) | 否 | 无 | 无 | 联系电话，提交表单必填，数据库保存原值 |
| message | TEXT | 否 | 无 | 无 | 询盘内容，接口当前最多4000字符 |
| locale | VARCHAR(15) | 否 | 无 | 无 | 语言代码，接口当前仅允许zh/en |
| status | VARCHAR(20) | 否 | 'NEW' | 无 | NEW新询盘、PROCESSING处理中、CLOSED已关闭 |
| created_at | TIMESTAMP | 否 | CURRENT_TIMESTAMP | 无 | 询盘提交时间 |
| country | VARCHAR(80) | 否 | 空字符串 | V2新增 | 国家/地区代码，选填 |
| industry | VARCHAR(80) | 否 | 空字符串 | V2新增 | 行业代码，选填 |
| product | VARCHAR(180) | 否 | 空字符串 | V2新增 | 选中的产品ID，选填 |
| website | VARCHAR(500) | 否 | 空字符串 | V2新增 | 客户网站，HTTP/HTTPS，选填 |
| company | VARCHAR(180) | 否 | 空字符串 | V2新增 | 公司名称，选填 |

索引：仅`PRIMARY KEY (id)`，当前没有邮箱唯一约束，同一客户可多次询盘。
数据库保存联系方式原值；仅授权管理员列表、单条详情和Excel导出返回完整邮箱/电话。
列表及导出按`created_at DESC, id DESC`排序；日期范围包含起止日全天，使用数据库服务器时间。不需要重新导入表结构。

### 表关系与扩展

`content_record.image_id`逻辑关联`media_record.id`，当前未设置FOREIGN KEY，
不能依赖数据库自动阻止孤立引用或自动级联删除。
内容类型、状态和语言代码均为字符串，允许值由Java校验，SQL未声明ENUM或CHECK。
管理员账号及密码目前通过配置提供，没有管理员用户表。
后续新增字段、表、索引后，应同步更新本节字段表。

## B步：预留增添空间

`templates/next_migration.sql.example`预留新增表、字段、数据回填与索引区域，
属于非执行模板，不会修改数据库。

确认新需求后，在Flyway目录创建`V2__功能英文描述.sql`，后续依次使用V3、V4。
已经执行的V1及其他迁移不可编辑，否则Flyway校验和会不一致。

后续可增加翻译子表、产品规格表、管理员权限表、询盘跟进表和媒体审核字段。
当前没有添加含义不明的备用字段，也没有预建空业务表。
新增字段或表时同步更新Entity、Mapper、DTO/VO、校验、测试和接口文档。

## C步：当前运行环境

当前本地开发使用MySQL，已有手动导入的V1结构，Flyway暂禁用。
已备份本地询盘表并手动追加V2的五个字段；不建立未经核实的Flyway基线。
新数据库先导入`gww_mysql_init.sql`，再执行
`../server/src/main/resources/db/migration/V2__inquiry_contact_fields.sql`。
已升级的库不要重复执行V2；应用Flyway接管前需核对实际V1/V2结构及基线版本。
网站设置复用`content_record`中`id=site-basic,kind=settings`固定记录，
`description`保存公开设置JSON，不存密码、密钥或磁盘路径。不创建额外空表。
