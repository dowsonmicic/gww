-- 官网后台初始化脚本，适用于MySQL 8及全新的独立数据库。
-- 不包含DROP、TRUNCATE或演示数据，不修改已有表。
-- 本文件用于手动初始化；应用自动建表请使用Flyway，二者不要重复使用。
-- 若手动导入后再启用Flyway，需人工确认结构与V1一致后设置版本1基线。

CREATE DATABASE IF NOT EXISTS gww
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gww;

-- 内容类型：products/categories/brands/articles/settings。
-- 状态：DRAFT/PUBLISHED/ARCHIVED；说明按纯文本存储。
CREATE TABLE content_record (
  id VARCHAR(36) NOT NULL COMMENT '内容UUID',
  kind VARCHAR(20) NOT NULL COMMENT '内容类型',
  name VARCHAR(120) NOT NULL COMMENT '中文名称',
  name_en VARCHAR(180) NOT NULL COMMENT '英文名称',
  category VARCHAR(80) NOT NULL COMMENT '材料分类',
  brand VARCHAR(80) NOT NULL COMMENT '品牌',
  status VARCHAR(20) NOT NULL COMMENT '发布状态',
  description TEXT NOT NULL COMMENT '中文说明',
  description_en TEXT NOT NULL COMMENT '英文说明',
  image_id VARCHAR(36) DEFAULT NULL COMMENT '已发布媒体ID，服务层校验引用',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间，由应用更新',
  PRIMARY KEY (id),
  KEY idx_content_kind_status (kind, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='官网内容';

-- 仅保存媒体标识与元数据，不保存服务器绝对目录。
-- 目录由MEDIA_PRIVATE_ROOT、MEDIA_PUBLIC_ROOT配置。
CREATE TABLE media_record (
  id VARCHAR(36) NOT NULL COMMENT '媒体UUID',
  object_key VARCHAR(100) NOT NULL COMMENT '随机文件名，相对于媒体根目录',
  original_name VARCHAR(180) NOT NULL COMMENT '原始文件名，仅展示使用',
  content_type VARCHAR(80) NOT NULL COMMENT '服务端检测的MIME类型',
  size_bytes BIGINT NOT NULL COMMENT '文件大小，字节',
  published BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否已公开发布',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_media_object_key (object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='媒体资源';

-- 联系方式保存原值，仅管理员可通过列表、详情和Excel导出读取。
CREATE TABLE inquiry_record (
  id VARCHAR(36) NOT NULL COMMENT '询盘UUID',
  name VARCHAR(80) NOT NULL COMMENT '客户名称',
  email VARCHAR(180) NOT NULL COMMENT '联系邮箱',
  phone VARCHAR(40) NOT NULL COMMENT '联系电话',
  message TEXT NOT NULL COMMENT '询盘内容',
  locale VARCHAR(15) NOT NULL COMMENT '语言代码，当前zh/en',
  status VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/PROCESSING/CLOSED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户询盘';

-- 后续扩展区：不要在已经导入的初始化文件中追加生产变更。
-- 新增功能通过独立V2、V3迁移实施，参考templates/next_migration.sql.example。
-- 可扩展方向：内容翻译子表、产品规格、管理员与权限、询盘跟进记录、
-- 媒体存储提供商与审核状态。以上仅预留设计方向，当前不创建占位字段。
