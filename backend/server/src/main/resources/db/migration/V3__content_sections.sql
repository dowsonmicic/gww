-- 产品详情结构化分段：JSON 数组，每段含 title/titleEn/body/bodyEn
ALTER TABLE content_record ADD COLUMN sections JSON NULL;
