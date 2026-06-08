-- ============================================================
-- SDPM 数据库建表脚本
-- 目标库   : MySQL 8.x（默认编码 utf8mb4，引擎 InnoDB）
-- 适用范围 : AI 辅助研发工作项流转与需求澄清系统
-- 文档     : docs/requirements/requirement.md
-- 规范     : 团队编码规范 §8 MySQL 规范
-- ============================================================

-- 1. 建库（幂等）
CREATE DATABASE IF NOT EXISTS `sdpm`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `sdpm`;

-- 关闭外键检查，方便一次性初始化
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 2. 工作项主表
-- ============================================================
DROP TABLE IF EXISTS `work_item`;
CREATE TABLE `work_item` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
    `code`            VARCHAR(64)  NOT NULL                          COMMENT '业务编码，例如 WI-001',
    `title`           VARCHAR(255) NOT NULL                          COMMENT '标题',
    `description`     TEXT         NULL                              COMMENT '详细描述',
    `type`            VARCHAR(16)  NOT NULL                          COMMENT '类型：STORY / BUG / TASK',
    `priority`        VARCHAR(8)   NOT NULL DEFAULT 'P2'             COMMENT '优先级：P0 / P1 / P2 / P3',
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'DRAFT'          COMMENT '状态机当前状态',
    `risk_level`      VARCHAR(16)  NULL                              COMMENT '风险等级：LOW / MEDIUM / HIGH',
    `assignee`        VARCHAR(64)  NULL                              COMMENT '负责人',
    `reporter`        VARCHAR(64)  NULL                              COMMENT '提出人',
    `tags`            JSON         NULL                              COMMENT '标签列表（结构化存储）',
    `acceptance_criteria` JSON     NULL                              COMMENT '验收标准列表',
    `version`         BIGINT       NOT NULL DEFAULT 0                COMMENT '乐观锁版本号',
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0                COMMENT '逻辑删除：0-未删，1-已删',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                                  ON UPDATE CURRENT_TIMESTAMP        COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_item_code` (`code`),
    KEY `idx_work_item_status`   (`status`),
    KEY `idx_work_item_type`     (`type`),
    KEY `idx_work_item_priority` (`priority`),
    KEY `idx_work_item_assignee` (`assignee`),
    KEY `idx_work_item_created`  (`created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci COMMENT='研发工作项（聚合根）';

-- ============================================================
-- 3. 状态流转历史
-- ============================================================
DROP TABLE IF EXISTS `work_item_status_history`;
CREATE TABLE `work_item_status_history` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
    `work_item_id`  BIGINT       NOT NULL                          COMMENT '所属工作项',
    `from_status`   VARCHAR(32)  NULL                              COMMENT '变更前状态（首建时为空）',
    `to_status`     VARCHAR(32)  NOT NULL                          COMMENT '变更后状态',
    `reason`        VARCHAR(500) NULL                              COMMENT '变更原因 / 备注',
    `operator`      VARCHAR(64)  NULL                              COMMENT '操作人（来自 X-User 请求头）',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_history_work_item` (`work_item_id`, `created_at`),
    KEY `idx_status_history_to`        (`to_status`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci COMMENT='工作项状态流转历史';

-- ============================================================
-- 4. 澄清问题
-- ============================================================
DROP TABLE IF EXISTS `clarification_question`;
CREATE TABLE `clarification_question` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
    `work_item_id`  BIGINT       NOT NULL                          COMMENT '所属工作项',
    `question`      VARCHAR(2000) NOT NULL                         COMMENT '问题内容',
    `severity`      VARCHAR(8)   NOT NULL DEFAULT 'P1'             COMMENT '严重程度：P0 / P1 / P2',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'OPEN'           COMMENT '解决状态：OPEN / RESOLVED',
    `answer`        VARCHAR(2000) NULL                             COMMENT '问题回答 / 处理说明',
    `raised_by`     VARCHAR(64)  NULL                              COMMENT '提出人',
    `resolved_by`   VARCHAR(64)  NULL                              COMMENT '解决人',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提出时间',
    `resolved_at`   DATETIME     NULL                              COMMENT '解决时间',
    PRIMARY KEY (`id`),
    KEY `idx_clarification_work_item` (`work_item_id`),
    KEY `idx_clarification_status`    (`status`),
    KEY `idx_clarification_severity`  (`severity`),
    KEY `idx_clarification_blocker`   (`work_item_id`, `severity`, `status`) COMMENT '支撑状态机阻断规则的高频查询'
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci COMMENT='工作项澄清问题';

-- ============================================================
-- 5. AI 分析结果
-- ============================================================
DROP TABLE IF EXISTS `ai_analysis_result`;
CREATE TABLE `ai_analysis_result` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
    `work_item_id`  BIGINT       NOT NULL                          COMMENT '所属工作项',
    `analysis_type` VARCHAR(32)  NOT NULL                          COMMENT '分析类型：SUMMARY / ACCEPTANCE / RISK / CLARIFICATION / TASK_BREAKDOWN',
    `payload`       JSON         NOT NULL                          COMMENT '结构化结果（key-value 列表，禁止散文）',
    `source`        VARCHAR(16)  NOT NULL DEFAULT 'MOCK'            COMMENT '来源：MOCK / LLM',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_analysis_work_item` (`work_item_id`, `created_at`),
    KEY `idx_ai_analysis_type`      (`analysis_type`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci COMMENT='AI 辅助分析结果';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 6. 测试初始化数据
-- ============================================================

-- 6.1 工作项（与 docs/sample-data/work-items.seed.json 对齐）
INSERT INTO `work_item`
    (`id`, `code`, `title`, `description`, `type`, `priority`, `status`,
     `risk_level`, `assignee`, `reporter`, `tags`, `acceptance_criteria`,
     `version`, `deleted`, `created_at`, `updated_at`)
VALUES
    (1, 'WI-001',
     '支持工作项从需求分析到开发完成的状态流转',
     '作为研发负责人，我希望工作项能够按照分析、准备、开发、测试和完成等阶段进行流转，以便跟踪研发进度并避免需求未澄清就进入开发。',
     'STORY', 'P1', 'DRAFT',
     NULL, 'candidate', 'product-owner',
     JSON_ARRAY('workflow', 'requirement'),
     JSON_ARRAY(
        '工作项只能按合法状态流转',
        '非法状态流转需要给出明确提示',
        '存在未解决高优先级澄清问题时不能进入后续开发状态'
     ),
     0, 0, '2026-06-01 09:00:00', '2026-06-01 09:00:00'),

    (2, 'WI-002',
     'AI 辅助生成需求澄清问题',
     '作为研发人员，我希望系统能够根据工作项描述生成可能需要澄清的问题，帮助我在开发前发现需求风险。',
     'STORY', 'P2', 'ANALYZING',
     NULL, 'candidate', 'product-owner',
     JSON_ARRAY('ai', 'clarification'),
     JSON_ARRAY(
        '可以触发 AI 分析',
        'AI 分析结果包含需求摘要、风险点或澄清问题',
        'AI 分析结果以结构化形式展示或返回'
     ),
     0, 0, '2026-06-01 10:00:00', '2026-06-01 10:00:00'),

    (3, 'WI-003',
     '修复登录页密码校验失败无提示问题',
     '登录页在密码错误时只刷新页面，没有给出任何错误提示，用户体验较差。',
     'BUG', 'P0', 'IN_PROGRESS',
     'HIGH', 'candidate', 'tester',
     JSON_ARRAY('login', 'frontend'),
     JSON_ARRAY(
        '密码错误时页面上展示明确错误提示',
        '连续 5 次失败后锁定 5 分钟',
        '补充单元测试覆盖该场景'
     ),
     0, 0, '2026-06-02 11:00:00', '2026-06-03 14:00:00'),

    (4, 'WI-004',
     '搭建 CI 基础流水线',
     '为后端仓库搭建 GitHub Actions 流水线，覆盖编译、单测、静态检查。',
     'TASK', 'P3', 'DONE',
     'LOW', 'candidate', 'tech-lead',
     JSON_ARRAY('ci', 'devops'),
     JSON_ARRAY(
        'PR 触发自动编译',
        '单测覆盖率不达标时阻断合并'
     ),
     0, 0, '2026-05-20 09:00:00', '2026-05-25 18:00:00');

-- 6.2 状态流转历史
INSERT INTO `work_item_status_history`
    (`work_item_id`, `from_status`, `to_status`, `reason`, `operator`, `created_at`)
VALUES
    (1, NULL,        'DRAFT',        '新建工作项',                'product-owner', '2026-06-01 09:00:00'),
    (2, NULL,        'DRAFT',        '新建工作项',                'product-owner', '2026-06-01 10:00:00'),
    (2, 'DRAFT',     'ANALYZING',    '进入待分析，准备触发 AI 摘要', 'candidate',     '2026-06-01 10:05:00'),
    (3, NULL,        'DRAFT',        '新建缺陷',                  'tester',        '2026-06-02 11:00:00'),
    (3, 'DRAFT',     'ANALYZING',    '确认复现路径',              'candidate',     '2026-06-02 11:30:00'),
    (3, 'ANALYZING', 'READY',        'P0 澄清问题已解决',         'candidate',     '2026-06-02 12:00:00'),
    (3, 'READY',     'IN_PROGRESS',  '开始修复',                  'candidate',     '2026-06-03 09:00:00'),
    (4, NULL,        'DRAFT',        '新建任务',                  'tech-lead',     '2026-05-20 09:00:00'),
    (4, 'DRAFT',     'ANALYZING',    NULL,                        'candidate',     '2026-05-20 09:30:00'),
    (4, 'ANALYZING', 'READY',        NULL,                        'candidate',     '2026-05-20 10:00:00'),
    (4, 'READY',     'IN_PROGRESS',  NULL,                        'candidate',     '2026-05-21 09:00:00'),
    (4, 'IN_PROGRESS','IN_TESTING',  NULL,                        'candidate',     '2026-05-24 17:00:00'),
    (4, 'IN_TESTING','DONE',         '测试通过，已合并发布',      'candidate',     '2026-05-25 18:00:00');

-- 6.3 澄清问题
-- WI-002：存在 P0 未解决澄清问题，演示"流转被阻断"；
-- WI-003：P0 已被解决，可进入 IN_PROGRESS；
-- WI-004：所有问题均已解决。
INSERT INTO `clarification_question`
    (`work_item_id`, `question`, `severity`, `status`, `answer`,
     `raised_by`, `resolved_by`, `created_at`, `resolved_at`)
VALUES
    (2,
     'AI 分析结果是否需要按角色（研发/测试/产品）差异化展示？',
     'P0', 'OPEN', NULL,
     'candidate', NULL,
     '2026-06-01 10:10:00', NULL),

    (2,
     'AI 摘要的最大长度限制是多少？',
     'P1', 'RESOLVED', '摘要控制在 200 字以内，关键字段单独提取。',
     'candidate', 'product-owner',
     '2026-06-01 10:11:00', '2026-06-01 10:30:00'),

    (2,
     '是否需要支持英文工作项？',
     'P2', 'OPEN', NULL,
     'candidate', NULL,
     '2026-06-01 10:12:00', NULL),

    (3,
     '密码错误提示需要 i18n 吗？',
     'P0', 'RESOLVED',
     '先支持中英文，文案放在前端资源文件。',
     'tester', 'candidate',
     '2026-06-02 11:10:00', '2026-06-02 11:50:00'),

    (3,
     '锁定时长是否需要可配置？',
     'P1', 'RESOLVED',
     '先用前端常量，后续再抽到配置中心。',
     'tester', 'candidate',
     '2026-06-02 11:11:00', '2026-06-02 11:55:00'),

    (4,
     'CI 运行环境是否使用自托管 Runner？',
     'P2', 'RESOLVED',
     '使用 GitHub 托管 Runner 即可。',
     'tech-lead', 'candidate',
     '2026-05-20 09:10:00', '2026-05-20 09:25:00');

-- 6.4 AI 分析结果
INSERT INTO `ai_analysis_result`
    (`work_item_id`, `analysis_type`, `payload`, `source`, `created_at`)
VALUES
    (2, 'SUMMARY',
     JSON_OBJECT(
        'summary', '为工作项提供 AI 摘要、风险识别与澄清问题生成能力，辅助研发前置澄清需求。',
        'keywords', JSON_ARRAY('AI', '需求澄清', '摘要', '风险点')
     ),
     'MOCK', '2026-06-01 10:06:00'),

    (2, 'RISK',
     JSON_OBJECT(
        'risks', JSON_ARRAY(
            JSON_OBJECT('level', 'MEDIUM', 'description', 'AI 摘要长度无统一规范，建议与产品确认上限。'),
            JSON_OBJECT('level', 'LOW',    'description', '当前需求不涉及多语言，但建议预留扩展。')
        )
     ),
     'MOCK', '2026-06-01 10:07:00'),

    (2, 'CLARIFICATION',
     JSON_OBJECT(
        'questions', JSON_ARRAY(
            JSON_OBJECT('question', 'AI 摘要输出是否需要中英双语？',     'severity', 'P1'),
            JSON_OBJECT('question', 'AI 能力是否需要按用户角色收敛？',   'severity', 'P1'),
            JSON_OBJECT('question', 'AI 失败时是否回退到人工流程？',     'severity', 'P2')
        )
     ),
     'MOCK', '2026-06-01 10:08:00'),

    (3, 'SUMMARY',
     JSON_OBJECT(
        'summary', '登录页密码错误时缺少用户提示，需要补充错误信息展示与防爆破锁定策略。',
        'keywords', JSON_ARRAY('登录', '密码校验', '用户体验', '安全')
     ),
     'MOCK', '2026-06-02 11:05:00'),

    (3, 'ACCEPTANCE',
     JSON_OBJECT(
        'criteria', JSON_ARRAY(
            '密码错误时在表单顶部展示红色错误提示',
            '连续失败 5 次后锁定账户 5 分钟',
            '单测覆盖错误码返回路径'
        )
     ),
     'MOCK', '2026-06-02 11:06:00'),

    (4, 'TASK_BREAKDOWN',
     JSON_OBJECT(
        'tasks', JSON_ARRAY(
            JSON_OBJECT('name', '配置 GitHub Actions 工作流', 'estimate', '0.5d'),
            JSON_OBJECT('name', '接入 JaCoCo 覆盖率报告',     'estimate', '0.5d'),
            JSON_OBJECT('name', '配置 PR 状态检查门禁',       'estimate', '0.5d')
        )
     ),
     'MOCK', '2026-05-20 09:35:00');

-- ============================================================
-- 7. 验证查询（可选执行）
-- ============================================================

-- SELECT wi.code, wi.title, wi.status,
--        (SELECT COUNT(1) FROM clarification_question cq
--          WHERE cq.work_item_id = wi.id
--            AND cq.severity = 'P0'
--            AND cq.status   = 'OPEN') AS p0_open_count
--   FROM work_item wi
--  ORDER BY wi.id;

-- SELECT id, analysis_type, source, created_at
--   FROM ai_analysis_result
--  ORDER BY id;
