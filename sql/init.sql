-- ============================================================
-- AI美业知识官 MySQL 初始化脚本
-- 数据库: beauty_knowledge
-- 字符集: utf8mb4
-- 说明: MySQL 8.0+，需配置 --ngram_token_size=2
-- ============================================================

CREATE DATABASE IF NOT EXISTS beauty_knowledge
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE beauty_knowledge;

-- ============================================================
-- 1) sys_user
-- ============================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user
(
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  username   VARCHAR(50)  NOT NULL COMMENT '用户名',
  password   VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  nickname   VARCHAR(50)           COMMENT '昵称',
  phone      VARCHAR(20)           COMMENT '手机号',
  role       VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色：admin/user',
  status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_role (role),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================================
-- 2) kb_category
-- ============================================================
DROP TABLE IF EXISTS kb_category;
CREATE TABLE kb_category
(
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
  parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父级ID，0为顶级分类',
  level       INT          NOT NULL DEFAULT 1 COMMENT '层级深度，从1开始',
  sort        INT          NOT NULL DEFAULT 0 COMMENT '同级排序，越小越靠前',
  icon        VARCHAR(200)          COMMENT '分类图标',
  description VARCHAR(500)          COMMENT '分类描述',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_parent_id (parent_id),
  KEY idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识分类表';

-- ============================================================
-- 3) kb_knowledge
-- ============================================================
DROP TABLE IF EXISTS kb_knowledge;
CREATE TABLE kb_knowledge
(
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '知识ID',
  title       VARCHAR(200) NOT NULL COMMENT '知识标题',
  content     LONGTEXT              COMMENT '知识正文内容',
  summary     VARCHAR(500)          COMMENT '内容摘要',
  category_id BIGINT       NOT NULL COMMENT '分类ID',
  type        VARCHAR(20)  NOT NULL DEFAULT 'article' COMMENT '类型：article/pdf/image',
  cover_url   VARCHAR(500)          COMMENT '封面URL',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '发布状态：1已发布 0草稿',
  view_count  INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
  author_id   BIGINT                COMMENT '作者ID',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_category_id (category_id),
  KEY idx_status (status),
  KEY idx_author_id (author_id),
  KEY idx_created_at (created_at),
  FULLTEXT INDEX ft_content (title, content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识内容表';

-- ============================================================
-- 4) kb_file
-- ============================================================
DROP TABLE IF EXISTS kb_file;
CREATE TABLE kb_file
(
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  knowledge_id   BIGINT                COMMENT '关联知识ID',
  original_name  VARCHAR(200) NOT NULL COMMENT '原始文件名',
  file_type      VARCHAR(20)  NOT NULL COMMENT '文件类型：pdf/image/doc/docx/txt',
  file_size      BIGINT                COMMENT '文件大小（字节）',
  minio_path     VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
  file_hash      VARCHAR(64)  NOT NULL COMMENT 'SHA-256 文件哈希',
  version        INT          NOT NULL DEFAULT 1 COMMENT '版本号',
  process_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/SUCCESS/FAILED',
  uploaded_by    BIGINT                COMMENT '上传人ID',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_file_hash (file_hash),
  KEY idx_knowledge_id (knowledge_id),
  KEY idx_process_status (process_status),
  KEY idx_uploaded_by (uploaded_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识文件表';

-- ============================================================
-- 5) kb_chunk
-- ============================================================
DROP TABLE IF EXISTS kb_chunk;
CREATE TABLE kb_chunk
(
  id           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '分块ID（对应Milvus向量主键）',
  file_id      BIGINT   NOT NULL COMMENT '来源文件ID',
  knowledge_id BIGINT            COMMENT '关联知识ID',
  chunk_index  INT      NOT NULL COMMENT '分块序号（从0开始）',
  content      TEXT     NOT NULL COMMENT '分块文本',
  page         INT               COMMENT '来源页码',
  char_count   INT               COMMENT '字符数',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_file_id (file_id),
  KEY idx_knowledge_id (knowledge_id),
  KEY idx_chunk_index (chunk_index),
  FULLTEXT INDEX ft_chunk_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识分块表';

-- ============================================================
-- 6) process_task
-- ============================================================
DROP TABLE IF EXISTS process_task;
CREATE TABLE process_task
(
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  file_id     BIGINT      NOT NULL COMMENT '文件ID',
  task_type   VARCHAR(50) NOT NULL DEFAULT 'KNOWLEDGE_PROCESS' COMMENT '任务类型',
  status      VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED',
  progress    INT         NOT NULL DEFAULT 0 COMMENT '进度：0-100',
  result_msg  TEXT                  COMMENT '结果描述/错误信息',
  retry_count INT         NOT NULL DEFAULT 0 COMMENT '已重试次数',
  max_retry   INT         NOT NULL DEFAULT 3 COMMENT '最大重试次数',
  started_at  DATETIME              COMMENT '开始时间',
  finished_at DATETIME              COMMENT '结束时间',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_file_id (file_id),
  KEY idx_status (status),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='处理任务表';

-- ============================================================
-- 7) chat_session
-- ============================================================
DROP TABLE IF EXISTS chat_session;
CREATE TABLE chat_session
(
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  user_id       BIGINT       NOT NULL COMMENT '用户ID',
  title         VARCHAR(200)          COMMENT '会话标题',
  message_count INT          NOT NULL DEFAULT 0 COMMENT '消息总数',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活跃时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话表';

-- ============================================================
-- 8) chat_message
-- ============================================================
DROP TABLE IF EXISTS chat_message;
CREATE TABLE chat_message
(
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  session_id    BIGINT      NOT NULL COMMENT '会话ID',
  role          VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
  content       LONGTEXT    NOT NULL COMMENT '消息内容',
  ref_sources   JSON                 COMMENT '引用来源JSON',
  intent        VARCHAR(20)          COMMENT '识别意图',
  tokens_used   INT                  COMMENT 'Token消耗',
  response_time INT                  COMMENT '响应耗时（毫秒）',
  created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_session_id (session_id),
  KEY idx_role (role),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

-- ============================================================
-- 9) beauty_ingredient
-- ============================================================
DROP TABLE IF EXISTS beauty_ingredient;
CREATE TABLE beauty_ingredient
(
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '成分ID',
  name         VARCHAR(100) NOT NULL COMMENT '成分名称',
  alias        VARCHAR(300)          COMMENT '别名，逗号分隔',
  description  LONGTEXT              COMMENT '成分说明',
  safety_level VARCHAR(20)           DEFAULT 'safe' COMMENT '安全等级：safe/caution/avoid',
  source       VARCHAR(50)           DEFAULT 'manual' COMMENT '来源：manual/auto_extract',
  confirmed    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否已确认：1是 0否',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name),
  KEY idx_safety_level (safety_level),
  KEY idx_confirmed (confirmed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美业成分表';

-- ============================================================
-- 10) beauty_effect
-- ============================================================
DROP TABLE IF EXISTS beauty_effect;
CREATE TABLE beauty_effect
(
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '功效ID',
  name        VARCHAR(100) NOT NULL COMMENT '功效名称',
  category    VARCHAR(50)           COMMENT '功效分类',
  description TEXT                  COMMENT '功效说明',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name),
  KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美业功效表';

-- ============================================================
-- 11) beauty_product
-- ============================================================
DROP TABLE IF EXISTS beauty_product;
CREATE TABLE beauty_product
(
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '产品ID',
  name         VARCHAR(200) NOT NULL COMMENT '产品名称',
  brand        VARCHAR(100)          COMMENT '品牌',
  category     VARCHAR(50)           COMMENT '产品分类',
  price_range  VARCHAR(50)           COMMENT '价格区间',
  skin_type    VARCHAR(100)          COMMENT '适合肤质',
  description  LONGTEXT              COMMENT '产品描述',
  knowledge_id BIGINT                COMMENT '关联知识ID',
  confirmed    TINYINT      NOT NULL DEFAULT 1 COMMENT '是否已确认：1是 0否',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_brand (brand),
  KEY idx_category (category),
  KEY idx_knowledge_id (knowledge_id),
  KEY idx_confirmed (confirmed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='美业产品表';

-- ============================================================
-- 12) rel_ingredient_effect
-- ============================================================
DROP TABLE IF EXISTS rel_ingredient_effect;
CREATE TABLE rel_ingredient_effect
(
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  ingredient_id BIGINT      NOT NULL COMMENT '成分ID',
  effect_id     BIGINT      NOT NULL COMMENT '功效ID',
  strength      VARCHAR(20) NOT NULL DEFAULT 'medium' COMMENT '强度：strong/medium/weak',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rel_ingredient_effect (ingredient_id, effect_id),
  KEY idx_ingredient_id (ingredient_id),
  KEY idx_effect_id (effect_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成分-功效关联表';

-- ============================================================
-- 13) rel_product_ingredient
-- ============================================================
DROP TABLE IF EXISTS rel_product_ingredient;
CREATE TABLE rel_product_ingredient
(
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  product_id    BIGINT      NOT NULL COMMENT '产品ID',
  ingredient_id BIGINT      NOT NULL COMMENT '成分ID',
  concentration VARCHAR(50)          COMMENT '浓度描述',
  is_key        TINYINT     NOT NULL DEFAULT 0 COMMENT '是否核心成分：1是 0否',
  PRIMARY KEY (id),
  UNIQUE KEY uk_rel_product_ingredient (product_id, ingredient_id),
  KEY idx_product_id (product_id),
  KEY idx_ingredient_id (ingredient_id),
  KEY idx_is_key (is_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='产品-成分关联表';

-- ============================================================
-- 14) entity_extract_pending
-- ============================================================
DROP TABLE IF EXISTS entity_extract_pending;
CREATE TABLE entity_extract_pending
(
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  file_id        BIGINT       NOT NULL COMMENT '文件ID',
  entity_type    VARCHAR(20)  NOT NULL COMMENT '实体类型：ingredient/effect/product',
  entity_name    VARCHAR(200) NOT NULL COMMENT '实体名称',
  source_text    TEXT                  COMMENT '来源上下文',
  extract_method VARCHAR(20)  NOT NULL DEFAULT 'llm' COMMENT '抽取方式：dict/llm',
  status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/CONFIRMED/REJECTED',
  confirmed_by   BIGINT                COMMENT '确认人ID',
  confirmed_at   DATETIME              COMMENT '确认时间',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_file_id (file_id),
  KEY idx_entity_type (entity_type),
  KEY idx_status (status),
  KEY idx_extract_method (extract_method),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实体抽取待确认表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 用户（BCrypt）
-- admin / admin
INSERT INTO sys_user (username, password, nickname, role, status)
VALUES ('admin', '$2a$10$XqB9sYGZHeRP3EzvM2QdJetKw5KoW8v8/UUPGtVz2Y68hLSQXMVrm', '系统管理员', 'admin', 1);

-- testuser / user
INSERT INTO sys_user (username, password, nickname, role, status)
VALUES ('testuser', '$2a$10$XE5Z1cxKcEFmi0bY9Bjdmuhp4y9ndN3jixZiAo4eXQ.C2xfcKtWz.', '测试用户', 'user', 1);

-- 4个顶级分类
INSERT INTO kb_category (name, parent_id, level, sort, icon, description)
VALUES ('护肤知识', 0, 1, 1, 'skin-care', '护肤相关知识'),
       ('美发技术', 0, 1, 2, 'hair-care', '美发相关知识'),
       ('美甲技术', 0, 1, 3, 'nail-art', '美甲相关知识'),
       ('医美项目', 0, 1, 4, 'medical-beauty', '医美相关知识');

-- 6个二级分类
INSERT INTO kb_category (name, parent_id, level, sort, description)
VALUES ('成分解析', 1, 2, 1, '护肤成分作用、安全性、搭配分析'),
       ('产品测评', 1, 2, 2, '护肤产品成分与效果测评'),
       ('护肤手法', 1, 2, 3, '护肤步骤与手法建议'),
       ('烫发技术', 2, 2, 1, '冷烫热烫等操作规范'),
       ('染发技术', 2, 2, 2, '染膏使用与色彩搭配'),
       ('护发知识', 2, 2, 3, '发质分析与护理方式');

-- 8个功效
INSERT INTO beauty_effect (name, category, description)
VALUES ('美白淡斑', '美白', '抑制黑色素、提亮肤色'),
       ('保湿补水', '保湿', '提升含水量、缓解干燥'),
       ('控油收缩毛孔', '控油', '调节油脂分泌'),
       ('抗氧化抗老', '抗老', '清除自由基、延缓衰老'),
       ('修复皮肤屏障', '修复', '强化角质屏障能力'),
       ('去角质', '清洁', '促进老废角质代谢'),
       ('舒缓抗敏', '修复', '缓解泛红和刺激'),
       ('促进胶原蛋白生成', '抗老', '提升弹性与紧致度');

-- 8个成分
INSERT INTO beauty_ingredient (name, alias, description, safety_level, source, confirmed)
VALUES ('烟酰胺', '维生素B3,Niacinamide', '常用美白与控油成分', 'safe', 'manual', 1),
       ('水杨酸', 'BHA,Salicylic Acid', '脂溶性酸，常用于控油祛痘', 'caution', 'manual', 1),
       ('玻尿酸', '透明质酸,Hyaluronic Acid', '经典保湿成分', 'safe', 'manual', 1),
       ('视黄醇', 'A醇,Retinol', '抗老明星成分，需注意耐受', 'caution', 'manual', 1),
       ('维生素C', 'VC,Ascorbic Acid', '抗氧化并提亮肤色', 'safe', 'manual', 1),
       ('积雪草', 'Centella Asiatica,CICA', '舒缓修复常用成分', 'safe', 'manual', 1),
       ('神经酰胺', 'Ceramide', '屏障修复核心脂质', 'safe', 'manual', 1),
       ('果酸', 'AHA,Glycolic Acid', '促进角质更新', 'caution', 'manual', 1);

-- 21条成分-功效关联
INSERT INTO rel_ingredient_effect (ingredient_id, effect_id, strength)
VALUES (1, 1, 'strong'),
       (1, 3, 'strong'),
       (1, 5, 'medium'),

       (2, 6, 'strong'),
       (2, 3, 'strong'),
       (2, 7, 'medium'),

       (3, 2, 'strong'),

       (4, 4, 'strong'),
       (4, 8, 'strong'),
       (4, 6, 'medium'),

       (5, 1, 'strong'),
       (5, 4, 'strong'),
       (5, 8, 'medium'),

       (6, 7, 'strong'),
       (6, 5, 'strong'),
       (6, 8, 'medium'),

       (7, 5, 'strong'),
       (7, 2, 'strong'),
       (7, 7, 'medium'),

       (8, 6, 'strong'),
       (8, 1, 'medium');

-- ============================================================
-- 验证查询（预期 14）
-- ============================================================
SELECT COUNT(*)
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'beauty_knowledge';
-- 预期结果: 14
