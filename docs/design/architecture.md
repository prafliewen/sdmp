# 系统设计方案：AI 辅助研发工作项流转与需求澄清系统

> 文档目的：在编码完成后（关键路径 M0~M10 已交付）输出本系统的整体技术设计方案，作为后续维护、扩展、评审、交接的统一蓝图。
> 适用对象：项目负责人、后端 / 前端研发、测试、新加入成员、AI 工具协作方。
> 输入文档：
> - [../requirements/requirement.md](../requirements/requirement.md)（需求理解）
> - [../tasks/breakdown.md](../tasks/breakdown.md)（任务拆解与里程碑）
> - [../database/db.sql](../database/db.sql)（建表脚本与种子数据）
> - [../../.trae/rules/group_development_rule.md](../../.trae/rules/group_development_rule.md)（团队编码规范）

---

## 1. 设计目标与非目标

### 1.1 设计目标

| 编号 | 目标 | 体现位置 |
|---|---|---|
| D1 | 后端架构清晰、分层严格、模块边界明确 | §2 架构、§3 模块边界 |
| D2 | 业务规则可被“读出来”，状态机与守卫链可被审计 | §4 数据模型、§5 调用链路 |
| D3 | AI 能力以接口 + 适配器模式封装，可 Mock/真实平滑切换 | §6 调用链路、§9 扩展性 |
| D4 | 异常处理统一、可定位、不泄露敏感信息 | §7 异常处理 |
| D5 | 安全最小化闭环（请求头用户上下文、参数校验、敏感日志屏蔽） | §8 安全设计 |
| D6 | 关键路径改动“只动一处”：邻接表、守卫链、AI 能力、字典、错误码 | §9 扩展性 |
| D7 | 与团队规范一致；不发明新规范 | 全文遵循 `group_development_rule.md` |

### 1.2 非目标（明确不做）

- 不做完整企业级 RBAC / 多租户 / 审计大盘。
- 不做看板拖拽、复杂前端工程。
- 不强制真实 LLM 接入。
- 不做分布式事务、消息中间件、分库分表。

---

## 2. 整体架构

### 2.1 架构分层

```text
┌──────────────────────────────────────────────────────────────────┐
│  表现层（Presentation）                                           │
│  - Vue 简单前端：列表 / 详情 / 流转弹窗 / 澄清弹窗 / AI 弹窗     │
│  - 统一 request 拦截：Result.code !== 0 → 统一 toast            │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTP / JSON
┌──────────────────────────▼───────────────────────────────────────┐
│  接口层（Controller）                                              │
│  - WorkItemController / TransitionController /                   │
│    ClarificationController / AiAnalysisController /               │
│    DictController / HealthController                              │
│  - 职责：参数接收（@Valid）→ 调用 Service → 返回 Result<T>       │
│  - 不写业务逻辑、不抛底层异常                                      │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│  业务层（Service）                                                 │
│  - WorkItemService / WorkItemTransitionService /                  │
│    ClarificationService / AiAnalysisService / DictService         │
│  - 状态机引擎 StateMachine + 守卫链 WorkItemTransitionGuard      │
│  - 跨表写操作 @Transactional                                       │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│  能力抽象层（AI Capability）                                        │
│  - AiCapability（接口）                                            │
│  - SummaryCapability / RiskCapability / ClarificationCapability    │
│  - AiAdapterRouter（MOCK / LLM 二选一）                            │
│  - MockAiAdapter / LlmAiAdapter（实现）                           │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│  持久层（Mapper / MyBatis-Plus）                                   │
│  - WorkItemMapper / WorkItemStatusHistoryMapper /                 │
│    ClarificationMapper / AiAnalysisResultMapper / DictMapper      │
│  - LambdaQueryWrapper 优先；XML 仅承载复杂 SQL                    │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│  存储层（MySQL 8）                                                 │
│  - work_item / work_item_status_history /                         │
│    clarification_question / ai_analysis_result /                  │
│    dict_item / user                                               │
└──────────────────────────────────────────────────────────────────┘

横切关注点（AOP / Filter / Interceptor / Listener）：
- GlobalExceptionHandler   统一异常
- UserContextFilter         X-User → UserContext
- MetaObjectHandler         create_time / update_time 自动填充
- springdoc-openapi         接口文档
- Logback（logback-spring.xml）按 profile 切分
```

### 2.2 部署形态（单进程）

```text
┌─────────────────────┐    ┌─────────────────────┐
│  Vue 静态资源        │    │  Spring Boot 单体    │
│  （nginx 或 vite 预览）│ ◄► │  （内嵌 Tomcat）     │
└─────────────────────┘    └──────────┬──────────┘
                                       │
                            ┌──────────▼──────────┐
                            │   MySQL 8           │
                            │   sdpm 库           │
                            └─────────────────────┘
```

- 单体应用 + 单库：符合"演示闭环"目标，避免引入不必要复杂度。
- profile = dev / demo：dev 指向本地开发库；demo 启动时自动注入种子数据，AI 强制走 MOCK。
- 后续可拆分为"工作项 + 状态机"和"AI 适配"两个微服务（见 §9.4）。

---

## 3. 模块边界

### 3.1 模块依赖图

```text
                ┌──────────────┐
                │   common     │  （无业务依赖：Result/PageResp/BizException/ErrorCode/UserContext）
                └──────┬───────┘
                       │
       ┌───────────────┼───────────────┐
       │               │               │
┌──────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│  workitem   │ │  state      │ │  dict       │
│  （聚合根）  │ │  machine    │ │  （字典）    │
└──────┬──────┘ └──────┬──────┘ └─────────────┘
       │               │
       │        ┌──────▼──────┐
       │        │ clarification│
       │        │  （守卫消费者）│
       │        └──────┬──────┘
       │               │
       │        ┌──────▼──────┐
       └───────►│    ai       │
                │  （能力抽象）│
                └─────────────┘
```

### 3.2 模块边界与职责

| 模块 | 职责 | 不允许做的事 | 依赖 |
|---|---|---|---|
| common | 统一返回、异常、错误码、用户上下文、工具 | 不含业务实体 | — |
| workitem | 工作项 CRUD、详情、列表；自身不写 `status` 流转 | 不调用状态机；不调用 AI | common |
| state machine | 状态邻接表、守卫链、状态历史写入 | 不直接 HTTP 暴露业务校验；不感知 AI | workitem, clarification |
| clarification | 澄清问题 CRUD + `P0 & OPEN` 计数查询 | 不写工作项 `status` | workitem |
| ai | 能力抽象、适配器路由、结果持久化 | 不改工作项实体；不感知状态机 | workitem |
| dict | 枚举 → 字典 label 映射；后台字典维护 | 不参与业务流转 | common |
| controller | 入参校验、调用 service、统一返回 | 不写业务逻辑 | 上述全部 |

### 3.3 接口契约边界

- **Controller ↔ Service**：用 DTO 入参 + VO 出参；Service 内部可使用内部 DTO；禁止 Entity 透出 Controller。
- **Service ↔ Mapper**：Entity ↔ Entity；Service 负责 Entity → DTO/VO 转换。
- **Service ↔ Service**：通过接口方法签名调用，避免循环依赖；用 `applicationContext.getBean(...)` 仅在守卫链中作为兜底。
- **AI ↔ WorkItem**：`AiCapability.analyse(WorkItemEntity)` 只读；不允许 AI 触发状态变更。

---

## 4. 数据模型

### 4.1 概念模型（ER）

```text
┌──────────────┐ 1   N ┌────────────────────────┐
│  work_item   │──────►│ work_item_status_history │
│ （聚合根）    │       └────────────────────────┘
│              │ 1   N ┌────────────────────────┐
│              │──────►│ clarification_question   │
│              │       └────────────────────────┘
│              │ 1   N ┌────────────────────────┐
│              │──────►│ ai_analysis_result       │
└──────────────┘       └────────────────────────┘

┌──────────────┐
│  dict_item   │   （独立字典表；不与 work_item 强耦合）
└──────────────┘

┌──────────────┐
│    user      │   （最小化；当前流程通过 X-User 请求头）
└──────────────┘
```

### 4.2 物理表设计

详见 [../database/db.sql](../database/db.sql)；下表为关键摘要。

#### 4.2.1 `work_item`（聚合根）

| 字段 | 类型 | 约束 | 业务含义 |
|---|---|---|---|
| id | BIGINT PK | AUTO_INCREMENT | 主键 |
| code | VARCHAR(64) | UNIQUE | 业务编码 `WI-001` |
| title | VARCHAR(255) | NOT NULL | 标题 |
| description | TEXT | NULL | 详细描述 |
| type | VARCHAR(16) | NOT NULL | `STORY/BUG/TASK` |
| priority | VARCHAR(8) | NOT NULL DEFAULT 'P2' | `P0/P1/P2/P3` |
| status | VARCHAR(32) | NOT NULL DEFAULT 'DRAFT' | 状态机当前态 |
| risk_level | VARCHAR(16) | NULL | `LOW/MEDIUM/HIGH` |
| assignee | VARCHAR(64) | NULL | 负责人 |
| reporter | VARCHAR(64) | NULL | 提出人 |
| tags | JSON | NULL | 标签列表 |
| acceptance_criteria | JSON | NULL | 验收标准 |
| version | BIGINT | NOT NULL DEFAULT 0 | 乐观锁 |
| deleted | TINYINT(1) | NOT NULL DEFAULT 0 | 逻辑删除 |
| created_at / updated_at | DATETIME | 自动 | 通用字段 |

索引：`(status)`、`(type)`、`(priority)`、`(assignee)`、`(created_at)`、`UNIQUE(code)`。

#### 4.2.2 `work_item_status_history`（状态历史）

| 字段 | 类型 | 业务含义 |
|---|---|---|
| id | BIGINT PK | 主键 |
| work_item_id | BIGINT | 所属工作项 |
| from_status | VARCHAR(32) NULL | 变更前；首建为 NULL |
| to_status | VARCHAR(32) NOT NULL | 变更后 |
| reason | VARCHAR(500) NULL | 变更原因 / 备注 |
| operator | VARCHAR(64) NULL | 操作人（来自 `X-User`） |
| created_at | DATETIME | 变更时间 |

索引：`(work_item_id, created_at)`、`(to_status)`。

#### 4.2.3 `clarification_question`（澄清问题）

| 字段 | 类型 | 业务含义 |
|---|---|---|
| id | BIGINT PK | 主键 |
| work_item_id | BIGINT | 所属工作项 |
| question | VARCHAR(2000) | 问题内容 |
| severity | VARCHAR(8) DEFAULT 'P1' | `P0/P1/P2` |
| status | VARCHAR(16) DEFAULT 'OPEN' | `OPEN/RESOLVED` |
| answer | VARCHAR(2000) NULL | 解决时的回答 |
| raised_by / resolved_by | VARCHAR(64) NULL | 提出人 / 解决人 |
| created_at / resolved_at | DATETIME | 时间字段 |

索引：`(work_item_id)`、`(status)`、`(severity)`、`(work_item_id, severity, status)` —— 后者专门支撑状态机守卫的高频查询。

#### 4.2.4 `ai_analysis_result`（AI 分析结果）

| 字段 | 类型 | 业务含义 |
|---|---|---|
| id | BIGINT PK | 主键 |
| work_item_id | BIGINT | 所属工作项 |
| analysis_type | VARCHAR(32) | `SUMMARY/ACCEPTANCE/RISK/CLARIFICATION/TASK_BREAKDOWN` |
| payload | JSON NOT NULL | 结构化结果（key-value 列表） |
| source | VARCHAR(16) DEFAULT 'MOCK' | `MOCK/LLM` |
| created_at | DATETIME | 生成时间 |

索引：`(work_item_id, created_at)`、`(analysis_type)`。

#### 4.2.5 `dict_item`（字典）

| 字段 | 类型 | 业务含义 |
|---|---|---|
| id | BIGINT PK | 主键 |
| type | VARCHAR(32) | 字典类型（如 `WORK_ITEM_STATUS`） |
| key | VARCHAR(32) | 字典项 key |
| label | VARCHAR(64) | 展示名 |
| value | VARCHAR(255) NULL | 业务值（可与 key 相同或扩展） |
| sort | INT DEFAULT 0 | 排序 |
| enabled | TINYINT(1) DEFAULT 1 | 是否启用 |
| created_at / updated_at | DATETIME | 通用字段 |

索引：`(type, sort)`、`UNIQUE(type, key)`。

### 4.3 关键不变量

1. **状态机闭合**：`(work_item.status, work_item_status_history)` 一一可追溯。
2. **P0 拦截数据约束**：`clarification_question(work_item_id, severity='P0', status='OPEN')` 计数 > 0 时，`work_item.status` 不允许进入 `{READY, IN_PROGRESS, IN_TESTING, DONE}`。
3. **AI 强 schema**：`ai_analysis_result.payload` 必须是合法 JSON，且符合 `AiAnalysisPayload` schema（由代码侧 Jackson + 校验保证）。
4. **软删除一致**：`work_item.deleted = 1` 的记录不出现在列表 / 详情中，但状态历史与澄清、AI 记录保留可追溯。
5. **乐观锁**：`work_item.version` 在更新时由 MyBatis-Plus `@Version` 自动维护，避免并发覆盖。

---

## 5. 调用链路

### 5.1 创建工作项

```text
Client
  │ POST /api/v1/work-items  { title, type, priority, description, ... }
  ▼
WorkItemController.create(WorkItemCreateReqDTO)
  │ 1. @Valid 校验
  │ 2. 注入 UserContext.operator
  ▼
WorkItemService.createWorkItem(req)
  │ 3. 业务规则：code 唯一 / 默认 status=DRAFT
  │ 4. 写 work_item
  │ 5. 写 work_item_status_history（from=null → to=DRAFT）
  │ 6. 同一 @Transactional
  ▼
Mapper.insert / Mapper.insertHistory
  │
  ▼
Result<WorkItemRespVO>
```

### 5.2 状态流转（含 P0 拦截）

```text
Client
  │ POST /api/v1/work-items/{id}/transitions  { targetStatus, reason }
  ▼
WorkItemTransitionController.transit(id, req)
  ▼
WorkItemTransitionService.transit(id, target, reason)
  │ 1. 加载 WorkItemEntity + 关联聚合（P0 OPEN 计数）
  │ 2. StateMachine.assertTransit(current, target)        // 邻接表校验
  │    - DONE → * / * → DONE 之外的非法跳级 → BIZ_TRANSITION_NOT_ALLOWED
  │    - DONE 源/目标 → BIZ_DONE_IMMUTABLE
  │ 3. 遍历 WorkItemTransitionGuard 链：
  │    - P0ClarificationGuard.check(current, target)
  │      若 target ∈ {READY, IN_PROGRESS, IN_TESTING, DONE}
  │         且 count(P0 & OPEN) > 0  →  BIZ_P0_CLARIFICATION_BLOCKED
  │ 4. 更新 work_item.status（带 @Version 自增）
  │ 5. 写 work_item_status_history（from → to + reason + operator）
  │ 6. 同一 @Transactional
  ▼
Result<{ newStatus, history }>
```

### 5.3 澄清问题管理

```text
新增：
POST /api/v1/work-items/{id}/clarifications
  → ClarificationService.addQuestion(workItemId, dto)
  → Mapper.insert
  → 返回 ClarificationRespVO

解决：
PUT /api/v1/clarifications/{cid}
  → ClarificationService.resolveQuestion(cid, dto)
  → 仅写入 answer / resolvedBy / resolvedAt / status=RESOLVED
  → 严禁修改 work_item.status
  → 解决后状态机守卫实时放行（无需额外触发）

查询：
GET /api/v1/work-items/{id}/clarifications?severity=&status=
  → ClarificationService.listQuestions(workItemId, filter)
  → LambdaQueryWrapper 组装
  → 返回 List<ClarificationRespVO>
```

### 5.4 AI 分析触发

```text
Client
  │ POST /api/v1/work-items/{id}/ai-analyses  { analysisType }
  ▼
AiAnalysisController.trigger(id, req)
  ▼
AiAnalysisService.triggerAnalysis(workItemId, type)
  │ 1. 加载 WorkItemEntity
  │ 2. 定位 AiCapability（按 type 从 Map 取）
  │ 3. AiAdapterRouter.route(ai.source) 选 Mock / Llm
  │ 4. capability.analyse(workItem)
  │    返回 AiAnalysisPayload（结构化 Map）
  │ 5. Jackson 序列化 → 校验 schema
  │ 6. 写入 ai_analysis_result
  │ 7. 同一 @Transactional（adapter 外部调用 → 主事务前完成）
  ▼
Result<AiAnalysisRespVO>   // 立即返回结构化结果 + 摘要
```

### 5.5 字典查询

```text
GET /api/v1/dicts?type=WORK_ITEM_STATUS
  → DictService.listByType(type)
  → 内存缓存（可选 Caffeine）→ Mapper.selectList
  → 返回 List<DictRespVO>
```

### 5.6 端到端演示闭环（来自需求 §4.5）

```text
1. POST /work-items                       创建 WI
2. POST /work-items/{id}/clarifications   添加 P0 OPEN 澄清问题
3. POST /work-items/{id}/transitions      target=READY  → 400 BIZ_P0_CLARIFICATION_BLOCKED
4. PUT  /clarifications/{cid}             解决该 P0 问题
5. POST /work-items/{id}/transitions      target=READY  → 200 OK
6. POST /work-items/{id}/transitions      target=IN_PROGRESS → 200 OK
7. POST /work-items/{id}/ai-analyses      type=SUMMARY  → 200 + 结构化 payload
```

---

## 6. 关键设计决策

### 6.1 状态机：邻接表 + 守卫链

- **邻接表**集中在 `StateMachine` 的 `Map<Status, Set<Status>>`，新增状态只改一处。
- **守卫链**以 `List<WorkItemTransitionGuard>` 顺序执行，每条规则一个类；新增规则加一个 `@Component` 即可，不动主流程。
- **DONE 不可变**放在邻接表与守卫双重保障：`DONE` 源/目标均不出现在邻接表，守卫中再次拦截作为兜底（防止误配）。

### 6.2 AI 能力：接口 + 适配器 + 强 schema

- `AiCapability` 接口强约束返回 `AiAnalysisPayload`（即 `Map<String, Object>` 强类型包装），禁止 `String` 散文。
- `AiAdapterRouter` 读取 `ai.source = mock | llm`，无侵入切换；切换不要求改业务代码。
- 适配器输出在 `AiAnalysisService` 统一做 schema 校验（必填 key 缺失直接抛 `BIZ_PARAM_INVALID`）。
- `MockAiAdapter` 基于规则模板：分析类型 → 模板，输出固定 schema；LLM 模式下 prompt 模板外置到 `application-*.yml`。

### 6.3 错误码：业务 / 系统分离

- 业务错误码统一 `BIZ_` 前缀：`BIZ_PARAM_INVALID`、`BIZ_NOT_FOUND`、`BIZ_TRANSITION_NOT_ALLOWED`、`BIZ_P0_CLARIFICATION_BLOCKED`、`BIZ_DONE_IMMUTABLE`。
- 系统错误码：`SYS_INTERNAL`。
- 错误码在 `ErrorCode` 枚举集中管理，全局异常处理器统一翻译为 `Result.error(code, message)`。

### 6.4 用户上下文：请求头 + ThreadLocal

- `UserContextFilter` 解析 `X-User` 请求头写入 `UserContext` ThreadLocal；缺省 `anonymous`。
- Service / 状态历史 / 澄清 / AI 触发时从 `UserContext` 读取 `operator`，不依赖前端 Cookie / Session。
- 过滤器在请求结束后必须 `remove()`，避免线程复用导致串扰。

### 6.5 事务边界

- 跨多表写操作必须 `@Transactional`：
  - `WorkItemService.createWorkItem`：写 `work_item` + `work_item_status_history`。
  - `WorkItemTransitionService.transit`：写 `work_item` + `work_item_status_history`。
  - `AiAnalysisService.triggerAnalysis`：写 `ai_analysis_result`（外部 AI 调用必须在主事务前完成或使用 `TransactionSynchronizationManager` 注册 afterCommit）。
- 单表 CRUD 不强制 `@Transactional`，避免不必要的事务开销。

### 6.6 分层与对象转换

- **Entity ↔ DB**：MyBatis-Plus 持久化。
- **Entity ↔ DTO/VO**：在 Service 层完成；Controller 不做转换。
- **DTO ↔ Controller**：Controller 入参 DTO，出参 VO + `Result`。
- **AI Payload ↔ DB JSON**：Jackson `ObjectMapper` 序列化；读取时 `Map<String, Object>` 解析。

### 6.7 接口版本与路径

- 统一前缀 `/api/v1`。
- REST 风格：列表 `GET`、详情 `GET`、创建 `POST`、修改 `PUT`、子资源操作 `POST /resource/{id}/action`。
- 状态流转、澄清、AI 触发均作为工作项的子资源动作挂载，路径语义清晰。

---

## 7. 异常处理

### 7.1 异常体系

```text
Throwable
  └── Exception
        ├── BizException               业务异常（可预期）
        │     ├── ErrorCode code       错误码
        │     └── String message       友好提示
        │
        ├── MethodArgumentNotValidException   参数校验（Spring）
        ├── BindException                      参数绑定（Spring）
        ├── ConstraintViolationException       参数校验（JSR-303 编程式）
        └── Other RuntimeException             系统异常
```

### 7.2 处理策略

| 异常 | 处理器 | 错误码 | 响应 message | 日志级别 |
|---|---|---|---|---|
| `BizException` | `GlobalExceptionHandler` | `bizException.getCode()` | `bizException.getMessage()` | INFO（业务可预期） |
| `MethodArgumentNotValidException` / `BindException` | `GlobalExceptionHandler` | `BIZ_PARAM_INVALID` | 拼接字段级 message | WARN |
| `ConstraintViolationException` | `GlobalExceptionHandler` | `BIZ_PARAM_INVALID` | 字段级 message | WARN |
| `HttpMessageNotReadableException` | `GlobalExceptionHandler` | `BIZ_PARAM_INVALID` | "请求体不可读" | WARN |
| 其他 `Exception` | `GlobalExceptionHandler` | `SYS_INTERNAL` | "系统繁忙，请稍后再试"（不泄露堆栈） | ERROR（含完整堆栈） |

### 7.3 关键约定

1. **业务异常不打印堆栈**：直接返回错误码 + 友好 message；落 WARN 日志即可。
2. **系统异常记录 ERROR**：包含完整堆栈（仅服务端日志，不返回给客户端）。
3. **统一返回体**：所有异常经 `GlobalExceptionHandler` 翻译为 `Result<T>`，前端只需判断 `code`。
4. **错误码字典**：见 `ErrorCode` 枚举；变更需同步更新 `docs/api/error-codes.md`。
5. **不泄露敏感信息**：错误信息不得包含 SQL、堆栈、密码、token、内部路径。

### 7.4 业务异常抛出规范

- Service 层遇到业务不满足时**直接抛** `BizException`，由全局处理器统一翻译。
- Controller 不允许 `try/catch` 后吞掉异常；不允许直接返回 `Result.error(...)` 绕过处理器（除已校验并确认安全的极简场景）。
- 守卫链中每条规则独立抛 `BizException`，message 中带"数量 / 当前态"等关键诊断信息，便于前端展示。

---

## 8. 安全设计

> 本系统处于"内部工具、演示闭环"定位，安全目标是**最小化防御 + 不留隐患**，不做企业级安全体系。

### 8.1 鉴权与会话

- **无登录态**：通过请求头 `X-User` 传递用户标识，写入 `UserContext` ThreadLocal。
- **缺省值**：未携带 `X-User` 时为 `anonymous`；服务不依赖该值做关键判定。
- **未来扩展点**：若需登录，预留 `AuthFilter` 位置（在 `UserContextFilter` 之前），将 `X-User` 升级为 JWT 解析。

### 8.2 权限

- 当前不做 RBAC，鉴权诉求最小化。
- `admin` 操作（如字典维护、用户管理）通过请求头 `X-Role=admin` 简易识别；后续可接入 Spring Security 替换。

### 8.3 输入安全

- **强制参数校验**：所有 Controller 入参 DTO 加 `@Valid`；JSR-303 注解（`@NotBlank`/`@Size`/`@Min`/`@Max`）。
- **MyBatis-Plus 防注入**：所有查询走 `LambdaQueryWrapper`，禁止字符串拼接。
- **JSON 安全**：`ObjectMapper` 不启用 `default typing`，避免反序列化漏洞。
- **SQL 复杂查询**：使用 `@Select` 注解或 XML 参数化，禁用 `${}` 拼接。

### 8.4 输出安全

- **不泄露堆栈**：`GlobalExceptionHandler` 翻译系统异常为通用提示。
- **不打印敏感字段**：Logback 屏蔽 `password` / `token` / `idCard` 等关键字（见 `logback-spring.xml`）。
- **统一错误码 + 友好 message**：避免将后端表名、字段名、SQL 暴露给前端。

### 8.5 传输与存储

- 本地演示使用 HTTP；生产环境前必须启用 HTTPS（前置 Nginx / 网关）。
- 数据库密码不入代码、不入日志；通过环境变量或 Nacos / Spring Cloud Config 注入。
- `application-*.yml` 不提交敏感值到 Git；提供 `application-*-example.yml` 作为模板。

### 8.6 AI 适配层安全

- **不直拼 prompt 到 SQL / 文件系统**：prompt 模板从配置文件读取，禁止运行时拼接未转义用户输入。
- **适配器输入校验**：传入 AI 的工作项字段做长度限制（避免超长 prompt）。
- **结果校验**：AI 返回强制走 schema 校验，缺失关键字段直接拒绝并 WARN 日志，不入库。

### 8.7 日志与审计

- 关键业务日志：状态流转、澄清解决、AI 触发均记录 INFO（操作人 + 关键字段）。
- 异常日志：业务异常 WARN，系统异常 ERROR。
- 不打印 `X-User` 之外的用户敏感数据。
- 后续可补充审计表（`audit_log`），记录"谁、什么时间、改了什么"。

---

## 9. 扩展性设计

### 9.1 扩展点全景

```text
                     ┌──────────────────────┐
                     │   新增状态           │
                     │   → 改邻接表一处     │
                     └──────────────────────┘
                                 │
                                 ▼
┌──────────────────────┐  ┌──────────────────────┐
│   新增业务规则       │  │   新增 AI 能力        │
│   → 加一个 Guard 类  │  │   → 加一个 Capability│
└──────────────────────┘  └──────────────────────┘
                                 │
                                 ▼
                     ┌──────────────────────┐
                     │   切换 AI 来源        │
                     │   → 改 ai.source      │
                     └──────────────────────┘
                                 │
                                 ▼
                     ┌──────────────────────┐
                     │   新增字典项          │
                     │   → 写 dict_item 一行 │
                     └──────────────────────┘
```

### 9.2 状态机扩展

- **新增状态**：
  1. 在 `WorkItemStatusEnum` 加枚举值。
  2. 在 `StateMachine` 邻接表加边。
  3. 视情况在 `P0ClarificationGuard` 调整 `target` 集合。
  4. 必要时在 `dict_item` 同步新增 label。
- **新增业务规则**：
  1. 实现 `WorkItemTransitionGuard` 接口，加 `@Component`。
  2. 在 `WorkItemTransitionService` 通过 `List<WorkItemTransitionGuard>` 注入（构造器注入自动收集）。
  3. 单测覆盖新规则的通过/拦截两条路径。

### 9.3 AI 能力扩展

- **新增能力**：
  1. `AiAnalysisTypeEnum` 加枚举。
  2. 新建 `XxxCapability implements AiCapability`，在 `supports()` 返回新类型。
  3. 加 `@Component`，`AiAnalysisService` 通过 `Map<AiAnalysisTypeEnum, AiCapability>` 自动收集。
  4. `MockAiAdapter` 加对应模板；`LlmAiAdapter` 加对应 prompt。
- **切换数据源**：`ai.source = mock | llm`，无侵入。
- **新增适配器**（如自研 RAG、本地规则引擎）：实现 `AiAdapter` 接口，在 `AiAdapterRouter` 注册即可。

### 9.4 模块与部署扩展

- **拆分为微服务**：
  - `sdpm-core`（workitem / state machine / clarification / dict）。
  - `sdpm-ai`（ai capability + adapter）。
  - 通信通过 OpenFeign / Dubbo；AI 服务独立扩缩容。
- **引入消息中间件**：
  - 状态流转后通过 `ApplicationEventPublisher` 发布 `WorkItemStatusChangedEvent`。
  - 后续可由 Notification / Audit 服务订阅，零侵入。
- **多租户**：
  - 在 `Entity` 中加 `tenant_id`，MyBatis-Plus 多租户插件自动拼接条件。
  - `UserContext` 增加 `tenantId` 字段。

### 9.5 字典与 i18n 扩展

- **新增枚举项**：在 `dict_item` 加行，label 可多语言（在 `i18n` 表或 `label_i18n` JSON 字段中维护）。
- **前端 i18n**：枚举展示名优先从 `/api/v1/dicts` 取，缺失时回退到枚举内置 desc。

### 9.6 可观测性扩展

- 当前：Logback + springdoc-openapi。
- 后续接入：
  - **指标**：Micrometer + Prometheus；按 Controller / Service / Mapper 维度暴露 QPS、RT、错误率。
  - **链路追踪**：Sleuth / OpenTelemetry；记录跨服务调用。
  - **健康检查**：`/actuator/health` 自定义健康指示器（DB、AI Adapter）。

### 9.7 测试扩展

- **单测**：JUnit 5 + Mockito；状态机 / 守卫链 / 适配器有专项用例。
- **集成测试**：`@SpringBootTest` + MySQL 数据库（`test` profile）。
- **冒烟**：`mvn -Psmoke verify` 一键跑通端到端。
- **契约测试**：后续可引入 Spring Cloud Contract，保障前后端契约。

---

## 10. 部署与运行

### 10.1 环境

| 维度 | 选择 |
|---|---|
| JDK | 21 |
| 构建 | Maven 3.9+ |
| 数据库 | MySQL 8.x |
| 端口 | 后端 `8080`；前端 vite 预览 `5173` |
| Profile | `dev`（本地开发） / `demo`（演示，注入种子数据 + Mock AI） |

### 10.2 启动顺序

1. 初始化数据库：`mysql -u root -p < docs/database/db.sql`。
2. 启动后端：`mvn spring-boot:run -Dspring-boot.run.profiles=demo`。
3. 启动前端：`cd frontend && npm install && npm run dev`。
4. 访问 `http://localhost:5173`，按 `README.md` 演示闭环走一遍。

### 10.3 关键配置项

| 配置 | 默认 | 说明 |
|---|---|---|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/sdpm?...` | dev/demo profile 各自配置 |
| `ai.source` | `mock` | `mock` / `llm` |
| `ai.llm.endpoint` | — | LLM 模式下的 API 地址 |
| `ai.llm.api-key` | — | 环境变量注入 |
| `logging.level.com.zaxxer.hikari` | `WARN` | 屏蔽敏感 logger |
| `mybatis-plus.configuration.log-impl` | dev 启用 / demo 关闭 | SQL 日志按需开启 |

---

## 11. 风险与缓解（设计层面）

| 编号 | 风险 | 设计缓解 |
|---|---|---|
| DR1 | 状态机改动扩散 | 邻接表集中在一处；新增规则只加 Guard 类 |
| DR2 | AI 工程化被字符串化 | `AiCapability` 强类型接口 + schema 校验；`AiAdapterRouter` 无侵入切换 |
| DR3 | 错误码混乱 | `ErrorCode` 枚举 + 全局处理器强制；PR 评审卡口 |
| DR4 | 跨表写一致性 | 关键路径 `@Transactional`；外部 AI 调用在事务前完成或注册 afterCommit |
| DR5 | 线程复用导致用户串扰 | `UserContextFilter` 在 finally 中 `remove()` |
| DR6 | 演示数据污染 | 种子数据通过 `demo` profile 注入；`dev` profile 不注入 |
| DR7 | 日志泄露 | Logback 屏蔽敏感关键字；`GlobalExceptionHandler` 翻译为通用提示 |
| DR8 | 前端请求未拦截 | 统一 `request` 实例拦截 `Result.code !== 0`；不直接 `axios` 调用 |

---

## 12. 后续演进路线

| 阶段 | 范围 | 与本设计的关系 |
|---|---|---|
| v1.0（当前） | workitem + 状态机 + 澄清 + AI（Mock） + 字典 + 简单前端 | 闭环可演示 |
| v1.1 | LLM 接入、OpenAPI 文档、用户上下文、乐观锁 | §9.3 / §8.1 扩展点 |
| v1.2 | 看板雏形、状态时间线、审计日志 | §9.4 / §9.6 扩展点 |
| v1.3 | 多租户、消息通知、外部系统集成（CI / 监控） | §9.4 扩展点 |
| v2.0 | 微服务拆分、可观测性、契约测试 | §9.4 / §9.7 扩展点 |

---

## 13. 附录

### 13.1 错误码字典（与代码侧 `ErrorCode` 枚举保持一致）

| 错误码 | 含义 | HTTP 状态 |
|---|---|---|
| `BIZ_PARAM_INVALID` | 参数校验失败 | 400 |
| `BIZ_NOT_FOUND` | 资源不存在 | 404 |
| `BIZ_TRANSITION_NOT_ALLOWED` | 状态流转不合法（邻接表外） | 409 |
| `BIZ_P0_CLARIFICATION_BLOCKED` | 存在未解决 P0 澄清问题 | 409 |
| `BIZ_DONE_IMMUTABLE` | DONE 状态不可变更 | 409 |
| `SYS_INTERNAL` | 系统异常 | 500 |

### 13.2 API 路径速查

| 模块 | Method | Path | 说明 |
|---|---|---|---|
| workitem | POST | `/api/v1/work-items` | 创建 |
| workitem | PUT | `/api/v1/work-items/{id}` | 修改（不含 status） |
| workitem | GET | `/api/v1/work-items` | 分页查询 |
| workitem | GET | `/api/v1/work-items/{id}` | 详情 |
| transition | POST | `/api/v1/work-items/{id}/transitions` | 状态流转 |
| transition | GET | `/api/v1/work-items/{id}/transitions` | 状态历史 |
| clarification | POST | `/api/v1/work-items/{id}/clarifications` | 新增澄清问题 |
| clarification | PUT | `/api/v1/clarifications/{cid}` | 解决澄清问题 |
| clarification | GET | `/api/v1/work-items/{id}/clarifications` | 列表查询 |
| ai | POST | `/api/v1/work-items/{id}/ai-analyses` | 触发 AI 分析 |
| ai | GET | `/api/v1/work-items/{id}/ai-analyses` | AI 历史 |
| dict | GET | `/api/v1/dicts` | 字典查询 |
| dict | POST/PUT/DELETE | `/api/v1/dicts[/...]` | 字典管理（admin） |
| health | GET | `/actuator/health` | 健康检查 |

### 13.3 关联文档索引

- 需求理解：[../requirements/requirement.md](../requirements/requirement.md)
- 任务拆解：[../tasks/breakdown.md](../tasks/breakdown.md)
- 数据库脚本：[../database/db.sql](../database/db.sql)
- 编码规范：[../../.trae/rules/group_development_rule.md](../../.trae/rules/group_development_rule.md)
- 过程记录：[../process.md](../process.md)
- AI 使用记录：[../ai-usage.md](../ai-usage.md)

---

> 本设计为 v1.0 版本，随 M11 加分项与后续演进同步更新；任何对邻接表、守卫链、AI 能力、错误码的修改，请同步更新本文档对应章节。
