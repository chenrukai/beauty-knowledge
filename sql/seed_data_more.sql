-- ============================================================
-- AI美业知识官 - 扩展测试数据脚本（可重复执行）
-- 文件: sql/seed_data_more.sql
-- 用途: 在已有表结构基础上补充较多测试数据
-- ============================================================

USE beauty_knowledge;
SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1) 用户数据
-- ------------------------------------------------------------
-- admin/admin, testuser/testuser（BCrypt）
INSERT INTO sys_user (username, password, nickname, phone, role, status)
VALUES
  ('admin', '$2a$10$71nr27YBIUTmN343YBagZOhnwvmLXldXQN80IdrypQwWrz4VFOV/G', '系统管理员', NULL, 'admin', 1),
  ('testuser', '$2a$10$0TyJlCHT94zaC9IvXhWNkO7CKqHd.10ppkmi18kLfE8GxinXAEX0G', '测试用户', NULL, 'user', 1)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  nickname = VALUES(nickname),
  role = VALUES(role),
  status = VALUES(status),
  updated_at = NOW();

-- 扩展20个普通用户（统一密码: 123456）
INSERT INTO sys_user (username, password, nickname, phone, role, status, created_at, updated_at)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 20
)
SELECT
  CONCAT('demo', LPAD(n, 2, '0')),
  '$2a$10$ePKNoLFxRTw4HV6ezFAbd.TW91PdJ0wAe0/98s1AxlBYLL4E/4ZRa',
  CONCAT('演示用户', LPAD(n, 2, '0')),
  CONCAT('1380000', LPAD(n, 4, '0')),
  'user',
  1,
  NOW(),
  NOW()
FROM seq
ON DUPLICATE KEY UPDATE
  nickname = VALUES(nickname),
  phone = VALUES(phone),
  status = VALUES(status),
  updated_at = NOW();

-- ------------------------------------------------------------
-- 2) 分类数据
-- ------------------------------------------------------------
INSERT INTO kb_category (name, parent_id, level, sort, icon, description, created_at)
SELECT '身体护理', 0, 1, 5, 'body-care', '身体护理相关知识', NOW()
WHERE NOT EXISTS (SELECT 1 FROM kb_category WHERE name = '身体护理');

INSERT INTO kb_category (name, parent_id, level, sort, icon, description, created_at)
SELECT '彩妆技巧', 0, 1, 6, 'makeup', '彩妆与妆容设计知识', NOW()
WHERE NOT EXISTS (SELECT 1 FROM kb_category WHERE name = '彩妆技巧');

SET @skin_id = (SELECT id FROM kb_category WHERE name = '护肤知识' ORDER BY id LIMIT 1);
SET @hair_id = (SELECT id FROM kb_category WHERE name = '美发技术' ORDER BY id LIMIT 1);
SET @nail_id = (SELECT id FROM kb_category WHERE name = '美甲技术' ORDER BY id LIMIT 1);
SET @med_id = (SELECT id FROM kb_category WHERE name = '医美项目' ORDER BY id LIMIT 1);
SET @body_id = (SELECT id FROM kb_category WHERE name = '身体护理' ORDER BY id LIMIT 1);
SET @makeup_id = (SELECT id FROM kb_category WHERE name = '彩妆技巧' ORDER BY id LIMIT 1);

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '敏感肌护理', @skin_id, 2, 4, '敏感肌护理与修复建议', NOW()
WHERE @skin_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name = '敏感肌护理');

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '头皮养护', @hair_id, 2, 4, '头皮清洁、维稳与护理', NOW()
WHERE @hair_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name = '头皮养护');

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '甲面修复', @nail_id, 2, 2, '甲面受损修复与护理', NOW()
WHERE @nail_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name = '甲面修复');

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '光电术后护理', @med_id, 2, 2, '光电项目术后护理建议', NOW()
WHERE @med_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name = '光电术后护理');

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '身体去角质', @body_id, 2, 1, '身体去角质与屏障维护', NOW()
WHERE @body_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name = '身体去角质');

INSERT INTO kb_category (name, parent_id, level, sort, description, created_at)
SELECT '底妆实操', @makeup_id, 2, 1, '底妆步骤与持妆技巧', NOW()
WHERE @makeup_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM kb_category WHERE name = '底妆实操');

-- ------------------------------------------------------------
-- 3) 知识内容（新增60条）
-- ------------------------------------------------------------
SET @author_id = (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1);
SET @c1 = (SELECT id FROM kb_category WHERE name = '成分解析' ORDER BY id LIMIT 1);
SET @c2 = (SELECT id FROM kb_category WHERE name = '产品测评' ORDER BY id LIMIT 1);
SET @c3 = (SELECT id FROM kb_category WHERE name = '护肤手法' ORDER BY id LIMIT 1);
SET @c4 = (SELECT id FROM kb_category WHERE name = '染发技术' ORDER BY id LIMIT 1);
SET @c5 = (SELECT id FROM kb_category WHERE name = '头皮养护' ORDER BY id LIMIT 1);
SET @c6 = (SELECT id FROM kb_category WHERE name = '底妆实操' ORDER BY id LIMIT 1);
SET @fallback_cat = (SELECT id FROM kb_category ORDER BY id LIMIT 1);

INSERT INTO kb_knowledge (
  title, content, summary, category_id, type, cover_url, status, view_count, author_id, created_at, updated_at
)
WITH RECURSIVE n AS (
  SELECT 1 AS i
  UNION ALL
  SELECT i + 1 FROM n WHERE i < 60
)
SELECT
  CONCAT('测试知识-', LPAD(i, 3, '0')),
  CONCAT('这是第', i, '篇测试知识内容。包含成分功效、护理步骤、风险提示与推荐搭配，用于分页、检索、详情和RAG测试。'),
  CONCAT('测试摘要-', LPAD(i, 3, '0')),
  CASE MOD(i, 6)
    WHEN 1 THEN COALESCE(@c1, @fallback_cat)
    WHEN 2 THEN COALESCE(@c2, @fallback_cat)
    WHEN 3 THEN COALESCE(@c3, @fallback_cat)
    WHEN 4 THEN COALESCE(@c4, @fallback_cat)
    WHEN 5 THEN COALESCE(@c5, @fallback_cat)
    ELSE COALESCE(@c6, @fallback_cat)
  END,
  'article',
  CONCAT('https://example.com/covers/', LPAD(i, 3, '0'), '.jpg'),
  1,
  FLOOR(RAND() * 5000),
  @author_id,
  DATE_SUB(NOW(), INTERVAL (60 - i) DAY),
  NOW()
FROM n
WHERE NOT EXISTS (
  SELECT 1 FROM kb_knowledge k WHERE k.title = CONCAT('测试知识-', LPAD(i, 3, '0'))
);

-- ------------------------------------------------------------
-- 4) 文件 + 任务 + 分块
-- ------------------------------------------------------------
INSERT IGNORE INTO kb_file (
  knowledge_id, original_name, file_type, file_size, minio_path, file_hash, version, process_status, uploaded_by, created_at, updated_at
)
SELECT
  k.id,
  CONCAT(REPLACE(k.title, '测试知识-', 'knowledge_'), '.pdf'),
  'pdf',
  102400 + k.id,
  CONCAT('pdf/2026/03/16/', k.id, '.pdf'),
  SHA2(CONCAT('kb-file-', k.id), 256),
  1,
  'SUCCESS',
  @author_id,
  NOW(),
  NOW()
FROM kb_knowledge k
WHERE k.title LIKE '测试知识-%'
LIMIT 40;

INSERT INTO process_task (
  file_id, task_type, status, progress, result_msg, retry_count, max_retry, started_at, finished_at, created_at, updated_at
)
SELECT
  f.id,
  'KNOWLEDGE_PROCESS',
  'SUCCESS',
  100,
  '处理完成',
  0,
  3,
  DATE_SUB(NOW(), INTERVAL 1 HOUR),
  NOW(),
  NOW(),
  NOW()
FROM kb_file f
LEFT JOIN process_task t ON t.file_id = f.id
WHERE f.original_name LIKE 'knowledge_%' AND t.id IS NULL;

-- 每个文件补3个chunk
INSERT INTO kb_chunk (file_id, knowledge_id, chunk_index, content, page, char_count, created_at)
SELECT
  f.id,
  f.knowledge_id,
  c.idx,
  CONCAT('文件', f.id, '分块', c.idx, '：本段用于向量检索和BM25测试，包含成分、功效、禁忌与使用建议。'),
  c.idx + 1,
  52,
  NOW()
FROM kb_file f
JOIN (
  SELECT 0 AS idx UNION ALL SELECT 1 UNION ALL SELECT 2
) c
LEFT JOIN kb_chunk kc ON kc.file_id = f.id AND kc.chunk_index = c.idx
WHERE f.original_name LIKE 'knowledge_%' AND kc.id IS NULL;

-- ------------------------------------------------------------
-- 5) 会话与消息
-- ------------------------------------------------------------
INSERT INTO chat_session (user_id, title, message_count, created_at, updated_at)
SELECT
  u.id,
  CONCAT('测试会话-', u.username),
  2,
  NOW(),
  NOW()
FROM sys_user u
WHERE u.username IN ('admin', 'testuser', 'demo01', 'demo02', 'demo03', 'demo04', 'demo05')
  AND NOT EXISTS (
    SELECT 1 FROM chat_session s WHERE s.title = CONCAT('测试会话-', u.username)
  );

INSERT INTO chat_message (session_id, role, content, ref_sources, intent, tokens_used, response_time, created_at)
SELECT
  s.id,
  'user',
  '敏感肌可以每天用酸吗？',
  JSON_ARRAY(),
  'consult',
  120,
  0,
  NOW()
FROM chat_session s
WHERE s.title LIKE '测试会话-%'
  AND NOT EXISTS (SELECT 1 FROM chat_message m WHERE m.session_id = s.id);

INSERT INTO chat_message (session_id, role, content, ref_sources, intent, tokens_used, response_time, created_at)
SELECT
  s.id,
  'assistant',
  '不建议每天使用高浓度酸类，可先低频建立耐受，并配合保湿修复。',
  JSON_ARRAY('测试知识-001', '测试知识-002'),
  'consult',
  380,
  680,
  NOW()
FROM chat_session s
WHERE s.title LIKE '测试会话-%'
  AND (SELECT COUNT(*) FROM chat_message m WHERE m.session_id = s.id) = 1;

UPDATE chat_session s
SET s.message_count = (SELECT COUNT(*) FROM chat_message m WHERE m.session_id = s.id)
WHERE s.title LIKE '测试会话-%';

-- ------------------------------------------------------------
-- 6) 功效/成分/产品与关系
-- ------------------------------------------------------------
INSERT IGNORE INTO beauty_effect (name, category, description, created_at)
VALUES
  ('提亮肤色', '美白', '改善暗沉和肤色不均', NOW()),
  ('改善细纹', '抗老', '减轻浅表细纹', NOW()),
  ('强韧发丝', '护发', '提升发丝韧性和光泽', NOW()),
  ('减少断发', '护发', '降低拉扯断裂概率', NOW());

INSERT IGNORE INTO beauty_ingredient (name, alias, description, safety_level, source, confirmed, created_at)
VALUES
  ('泛醇', '维生素原B5,Panthenol', '保湿舒缓常见成分', 'safe', 'manual', 1, NOW()),
  ('角鲨烷', 'Squalane', '亲肤保湿，增强润滑感', 'safe', 'manual', 1, NOW()),
  ('依克多因', 'Ectoin', '渗透压保护，提升耐受', 'safe', 'manual', 1, NOW()),
  ('咖啡因', 'Caffeine', '眼周与头皮产品常见成分', 'caution', 'manual', 1, NOW()),
  ('氨甲环酸', 'Tranexamic Acid', '常用于美白淡斑配方', 'caution', 'manual', 1, NOW()),
  ('壬二酸', 'Azelaic Acid', '改善痘印与泛红', 'caution', 'manual', 1, NOW());

INSERT IGNORE INTO rel_ingredient_effect (ingredient_id, effect_id, strength)
SELECT i.id, e.id, 'strong'
FROM beauty_ingredient i, beauty_effect e
WHERE i.name = '泛醇' AND e.name = '舒缓抗敏';

INSERT IGNORE INTO rel_ingredient_effect (ingredient_id, effect_id, strength)
SELECT i.id, e.id, 'strong'
FROM beauty_ingredient i, beauty_effect e
WHERE i.name = '角鲨烷' AND e.name = '保湿补水';

INSERT IGNORE INTO rel_ingredient_effect (ingredient_id, effect_id, strength)
SELECT i.id, e.id, 'medium'
FROM beauty_ingredient i, beauty_effect e
WHERE i.name = '依克多因' AND e.name = '修复皮肤屏障';

INSERT IGNORE INTO rel_ingredient_effect (ingredient_id, effect_id, strength)
SELECT i.id, e.id, 'medium'
FROM beauty_ingredient i, beauty_effect e
WHERE i.name = '氨甲环酸' AND e.name = '美白淡斑';

INSERT INTO beauty_product (name, brand, category, price_range, skin_type, description, knowledge_id, confirmed, created_at)
WITH RECURSIVE p AS (
  SELECT 1 AS i
  UNION ALL
  SELECT i + 1 FROM p WHERE i < 30
)
SELECT
  CONCAT('演示产品-', LPAD(i, 3, '0')),
  CONCAT('品牌', CHAR(64 + ((i - 1) % 6) + 1)),
  CASE MOD(i, 4)
    WHEN 1 THEN '精华'
    WHEN 2 THEN '面霜'
    WHEN 3 THEN '面膜'
    ELSE '防晒'
  END,
  CASE MOD(i, 3)
    WHEN 1 THEN '100-199'
    WHEN 2 THEN '200-399'
    ELSE '400+'
  END,
  CASE MOD(i, 3)
    WHEN 1 THEN '干皮'
    WHEN 2 THEN '油皮'
    ELSE '混合皮'
  END,
  CONCAT('用于列表、筛选与关系演示的测试产品 ', i),
  (SELECT id FROM kb_knowledge WHERE title = CONCAT('测试知识-', LPAD(i, 3, '0')) LIMIT 1),
  1,
  NOW()
FROM p
WHERE NOT EXISTS (SELECT 1 FROM beauty_product bp WHERE bp.name = CONCAT('演示产品-', LPAD(i, 3, '0')));

INSERT IGNORE INTO rel_product_ingredient (product_id, ingredient_id, concentration, is_key)
SELECT p.id, i.id, '2%', 1
FROM beauty_product p
JOIN beauty_ingredient i ON i.name = '烟酰胺'
WHERE p.name LIKE '演示产品-%';

INSERT IGNORE INTO rel_product_ingredient (product_id, ingredient_id, concentration, is_key)
SELECT p.id, i.id, '0.2%', 0
FROM beauty_product p
JOIN beauty_ingredient i ON i.name = '视黄醇'
WHERE p.name LIKE '演示产品-%' AND MOD(p.id, 2) = 0;

INSERT IGNORE INTO rel_product_ingredient (product_id, ingredient_id, concentration, is_key)
SELECT p.id, i.id, '1%', 0
FROM beauty_product p
JOIN beauty_ingredient i ON i.name = '泛醇'
WHERE p.name LIKE '演示产品-%';

-- ------------------------------------------------------------
-- 7) 待确认实体
-- ------------------------------------------------------------
INSERT INTO entity_extract_pending (
  file_id, entity_type, entity_name, source_text, extract_method, status, confirmed_by, confirmed_at, created_at
)
SELECT
  f.id,
  CASE MOD(f.id, 3)
    WHEN 0 THEN 'ingredient'
    WHEN 1 THEN 'effect'
    ELSE 'product'
  END,
  CONCAT('候选实体-', f.id),
  CONCAT('来源片段：文件', f.id, '包含可疑实体词条，等待人工确认。'),
  'llm',
  'PENDING',
  NULL,
  NULL,
  NOW()
FROM kb_file f
WHERE f.original_name LIKE 'knowledge_%'
  AND NOT EXISTS (SELECT 1 FROM entity_extract_pending e WHERE e.entity_name = CONCAT('候选实体-', f.id))
LIMIT 30;

-- ------------------------------------------------------------
-- 8) 验证统计
-- ------------------------------------------------------------
SELECT 'sys_user' AS table_name, COUNT(*) AS total FROM sys_user
UNION ALL SELECT 'kb_category', COUNT(*) FROM kb_category
UNION ALL SELECT 'kb_knowledge', COUNT(*) FROM kb_knowledge
UNION ALL SELECT 'kb_file', COUNT(*) FROM kb_file
UNION ALL SELECT 'kb_chunk', COUNT(*) FROM kb_chunk
UNION ALL SELECT 'process_task', COUNT(*) FROM process_task
UNION ALL SELECT 'chat_session', COUNT(*) FROM chat_session
UNION ALL SELECT 'chat_message', COUNT(*) FROM chat_message
UNION ALL SELECT 'beauty_ingredient', COUNT(*) FROM beauty_ingredient
UNION ALL SELECT 'beauty_effect', COUNT(*) FROM beauty_effect
UNION ALL SELECT 'beauty_product', COUNT(*) FROM beauty_product
UNION ALL SELECT 'rel_ingredient_effect', COUNT(*) FROM rel_ingredient_effect
UNION ALL SELECT 'rel_product_ingredient', COUNT(*) FROM rel_product_ingredient
UNION ALL SELECT 'entity_extract_pending', COUNT(*) FROM entity_extract_pending;
