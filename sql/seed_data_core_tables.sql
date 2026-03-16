USE beauty_knowledge;
SET NAMES utf8mb4;

-- admin/admin, testuser/testuser
INSERT INTO sys_user (username, password, nickname, phone, role, status)
VALUES
  ('admin', '$2a$10$71nr27YBIUTmN343YBagZOhnwvmLXldXQN80IdrypQwWrz4VFOV/G', '系统管理员', NULL, 'admin', 1),
  ('testuser', '$2a$10$0TyJlCHT94zaC9IvXhWNkO7CKqHd.10ppkmi18kLfE8GxinXAEX0G', '测试用户', NULL, 'user', 1)
ON DUPLICATE KEY UPDATE
  password = VALUES(password), nickname = VALUES(nickname), role = VALUES(role), status = VALUES(status), updated_at = NOW();

INSERT INTO sys_user (username, password, nickname, phone, role, status, created_at, updated_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 30
)
SELECT
  CONCAT('demo', LPAD(n, 2, '0')),
  '$2a$10$ePKNoLFxRTw4HV6ezFAbd.TW91PdJ0wAe0/98s1AxlBYLL4E/4ZRa',
  CONCAT('演示用户', LPAD(n, 2, '0')),
  CONCAT('1390000', LPAD(n, 4, '0')),
  'user',
  1,
  NOW(),
  NOW()
FROM seq
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), phone = VALUES(phone), status = VALUES(status), updated_at = NOW();

-- 分类
INSERT INTO kb_category (name, parent_id, level, sort, icon, description, created_at)
SELECT '护肤知识', 0, 1, 1, 'skin-care', '护肤相关知识', NOW()
WHERE NOT EXISTS (SELECT 1 FROM kb_category WHERE name='护肤知识');

INSERT INTO kb_category (name, parent_id, level, sort, icon, description, created_at)
SELECT '美发技术', 0, 1, 2, 'hair-care', '美发相关知识', NOW()
WHERE NOT EXISTS (SELECT 1 FROM kb_category WHERE name='美发技术');

INSERT INTO kb_category (name, parent_id, level, sort, icon, description, created_at)
SELECT '彩妆技巧', 0, 1, 3, 'makeup', '彩妆相关知识', NOW()
WHERE NOT EXISTS (SELECT 1 FROM kb_category WHERE name='彩妆技巧');

SET @skin_id = (SELECT id FROM kb_category WHERE name='护肤知识' ORDER BY id LIMIT 1);
SET @hair_id = (SELECT id FROM kb_category WHERE name='美发技术' ORDER BY id LIMIT 1);
SET @makeup_id = (SELECT id FROM kb_category WHERE name='彩妆技巧' ORDER BY id LIMIT 1);

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '成分解析', @skin_id, 2, 1, '成分原理与搭配建议', NOW()
WHERE @skin_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name='成分解析');

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '产品测评', @skin_id, 2, 2, '产品成分与体验评估', NOW()
WHERE @skin_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name='产品测评');

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '头皮养护', @hair_id, 2, 1, '头皮清洁与护理方法', NOW()
WHERE @hair_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name='头皮养护');

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '底妆实操', @makeup_id, 2, 1, '底妆技巧与持妆策略', NOW()
WHERE @makeup_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name='底妆实操');

SET @c1 = (SELECT id FROM kb_category WHERE name='成分解析' ORDER BY id LIMIT 1);
SET @c2 = (SELECT id FROM kb_category WHERE name='产品测评' ORDER BY id LIMIT 1);
SET @c3 = (SELECT id FROM kb_category WHERE name='头皮养护' ORDER BY id LIMIT 1);
SET @c4 = (SELECT id FROM kb_category WHERE name='底妆实操' ORDER BY id LIMIT 1);
SET @fallback_cat = (SELECT id FROM kb_category ORDER BY id LIMIT 1);
SET @admin_id = (SELECT id FROM sys_user WHERE username='admin' LIMIT 1);

-- 知识 120 条
INSERT INTO kb_knowledge (title, content, summary, category_id, type, cover_url, status, view_count, author_id, created_at, updated_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 120
)
SELECT
  CONCAT('测试知识-', LPAD(n, 3, '0')),
  CONCAT('第', n, '篇测试内容：包含成分、功效、步骤、注意事项，可用于分页、搜索和详情验证。'),
  CONCAT('测试摘要-', LPAD(n, 3, '0')),
  CASE MOD(n, 4)
    WHEN 1 THEN COALESCE(@c1, @fallback_cat)
    WHEN 2 THEN COALESCE(@c2, @fallback_cat)
    WHEN 3 THEN COALESCE(@c3, @fallback_cat)
    ELSE COALESCE(@c4, @fallback_cat)
  END,
  'article',
  CONCAT('https://example.com/covers/', LPAD(n, 3, '0'), '.jpg'),
  1,
  FLOOR(RAND() * 8000),
  @admin_id,
  DATE_SUB(NOW(), INTERVAL (120 - n) DAY),
  NOW()
FROM seq
WHERE NOT EXISTS (SELECT 1 FROM kb_knowledge k WHERE k.title = CONCAT('测试知识-', LPAD(n, 3, '0')));

-- 文件 80 条
INSERT IGNORE INTO kb_file (
  knowledge_id, original_name, file_type, file_size, minio_path, file_hash, version, process_status, uploaded_by, created_at, updated_at
)
SELECT
  k.id,
  CONCAT('knowledge_', LPAD(k.id, 4, '0'), '.pdf'),
  'pdf',
  204800 + k.id,
  CONCAT('pdf/2026/03/16/', k.id, '.pdf'),
  SHA2(CONCAT('kb-file-', k.id), 256),
  1,
  'SUCCESS',
  @admin_id,
  NOW(),
  NOW()
FROM kb_knowledge k
WHERE k.title LIKE '测试知识-%'
LIMIT 80;

SELECT 'sys_user' AS table_name, COUNT(*) AS total FROM sys_user
UNION ALL SELECT 'kb_category', COUNT(*) FROM kb_category
UNION ALL SELECT 'kb_knowledge', COUNT(*) FROM kb_knowledge
UNION ALL SELECT 'kb_file', COUNT(*) FROM kb_file;
