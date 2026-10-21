-- 既有询盘数据保留；新字段默认空字符串，兼容历史记录。
ALTER TABLE inquiry_record ADD COLUMN country VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE inquiry_record ADD COLUMN industry VARCHAR(80) NOT NULL DEFAULT '';
ALTER TABLE inquiry_record ADD COLUMN product VARCHAR(180) NOT NULL DEFAULT '';
ALTER TABLE inquiry_record ADD COLUMN website VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE inquiry_record ADD COLUMN company VARCHAR(180) NOT NULL DEFAULT '';
