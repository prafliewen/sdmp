# 任务拆解：AI 辅助研发工作项流转与需求澄清系统

> 文档目的：将 `docs/requirements/requirement.md` 拆解为可执行、可验收、可排期的任务清单。
> 拆分维度：**里程碑 → 模块 → 任务 → 子任务 → 验收标准**。
> 标注信息：**依赖关系**（前置/后置）、**优先级**（P0 关键路径 / P1 必做 / P2 加分 / P3 可选）。
> 业务模块实施顺序遵循用户偏好：**workitem → clarification → ai → dict**（dict 在业务规则稳定后落地）。

---

## 0. 总体里程碑地图

| 编号 | 里程碑 | 目标 | 关键产出 | 前置依赖 | 优先级 |
|---|---|---|---|---|---|
| M0 | 工程基础 | 仓库可启动、目录规范、配置分层 | Maven 工程、3 套 profile、`application.yml`、目录骨架 | — | P0 |
| M1 | 通用组件 | 统一返回、分页、异常、错误码、用户上下文 | `Result` / `PageResp` / `BizException` / `ErrorCode` / `UserContext` / `GlobalExceptionHandler` | M0 | P0 |
| M2 | 数据模型与字典 | 库表落地、枚举/字典可维护 | 6 张核心表 + 字典表、`db.sql`、种子数据 | M0, M1 | P0 |
| M3 | 工作项模块（workitem） | CRUD、详情、版本、列表查询 | `WorkItem` Entity/Mapper/Service/Controller/DTO/VO | M1, M2 | P0 |
| M4 | 状态流转（状态机） | 邻接表 + 守卫链 + 历史 | `StateMachine` / `WorkItemTransitionGuard` / 历史接口 | M3 | P0 |
| M5 | 澄清问题（clarification） | 新增/解决/查询 + P0 拦截消费 | `Clarification` 完整链路、联调 M4 守卫 | M3, M4 | P0 |
| M6 | AI 辅助分析（ai） | 能力抽象 + 至少 3 种能力 + Mock/可替换 | `AiCapability` / `MockAiAdapter` / 调度 Service / 历史接口 | M3 | P0 |
| M7 | 字典管理（dict） | 状态/类型/优先级/严重度后台可维护 | `Dict` 模块、字典查询接口 | M2 | P1 |
| M8 | 简单前端页面 | 端到端可演示闭环 | 单页 Vue（列表/详情/流转/澄清/AI） | M3~M6 | P1 |
| M9 | 测试与质量 | 核心规则 + 适配层单测全过 | 4 类单测、`mvn test` 全绿 | M3~M6 | P0 |
| M10 | 文档与交付 | 启动文档、过程记录、AI 使用记录 | README、process.md、ai-usage.md、demo 脚本 | M3~M9 | P0 |
| M11 | 加分项 | OpenAPI、乐观锁、用户上下文、Docker Compose、看板雏形 | 增量交付 | M3~M10 | P2 |

> 关键路径：**M0 → M1 → M2 → M3 → M4 → M5 → M9 → M10**。
> M6 与 M5 并行（AI 能力在 M4 完成后即可独立开发，不依赖 M5）。
> M7、M8、M11 在关键路径稳定后并行推进。

---

## M0 · 工程基础

> 目标：让仓库“能起来、目录对、配置分”，后续模块只关心业务，不重复造轮子。

### M0-MOD-1 Maven 工程与目录骨架

- **任务 M0-T1：初始化 Maven 工程**
  - 子任务：
    - 生成 `pom.xml`，声明 `spring-boot-starter-parent 3.x`、`java 21`、`mybatis-plus-boot-starter`、`mysql-connector-j`、`springdoc-openapi-starter-webmvc-ui`、`lombok`（可选）。
    - 锁定常用依赖版本（Jackson、Hutool/Guava 视需要）。
  - 验收：
    - [ ] `mvn -v` 在 JDK 21 下可执行。
    - [ ] `mvn compile` 通过。
    - [ ] `pom.xml` 注释清晰，依赖用途明确。
  - 依赖：—
  - 优先级：P0

- **任务 M0-T2：建立标准目录结构**
  - 子任务：
    - 后端：`backend/src/main/java/com/company/project/{common,config,controller,dto,entity,enum,exception,mapper,service,service/impl,vo,util}`。
    - 资源：`backend/src/main/resources/{mapper,application*.yml}`。
    - 测试：`backend/src/test/java/...`。
    - 前端：`frontend/{src/{api,components,views,router,store,utils,styles},public}`。
  - 验收：
    - [ ] 目录与 `group_development_rule.md §2` 一致。
    - [ ] 空包占位文件不留（避免 IDE 编译警告）。
  - 依赖：M0-T1
  - 优先级：P0

- **任务 M0-T3：日志与基础配置**
  - 子任务：
    - 引入 Logback，配置 `logback-spring.xml`，按 `dev/demo/prod` 拆分 appender。
    - 在配置中屏蔽 `com.zaxxer.hikari`、`org.apache.ibatis` 等敏感/冗余 logger。
    - 关键业务日志规范：INFO 正常、WARN 可预期、ERROR 系统异常。
  - 验收：
    - [ ] 启动日志有 banner 与端口。
    - [ ] 关键 logger 级别与规范一致。
    - [ ] 不打印 `password` / `token` / 身份证等敏感字段。
  - 依赖：M0-T2
  - 优先级：P0

### M0-MOD-2 多环境 profile

- **任务 M0-T4：3 套 Profile**
  - 子任务：
    - `application.yml`：通用配置（服务名、端口、MyBatis-Plus 基础设置）。
    - `application-dev.yml`：本地 MySQL dev 库、Mock AI。
    - `application-demo.yml`：演示用 MySQL + Mock AI + 预置种子数据开关。
  - 验收：
    - [ ] `--spring.profiles.active=dev|demo` 启动生效。
    - [ ] 数据源、JPA/MP 打印、端口可随 profile 切换。
  - 依赖：M0-T3
  - 优先级：P0

---

## M1 · 通用组件

> 目标：跨模块复用的“胶水”一次到位，业务模块只写业务。

### M1-MOD-1 统一返回与分页

- **任务 M1-T1：定义 `Result<T>`**
  - 子任务：
    - 字段：`code / message / data / timestamp`。
    - 静态方法：`success()` / `success(T)` / `error(code, message)` / `error(ErrorCode)`。
  - 验收：
    - [ ] 与规范 §6.2 完全一致。
    - [ ] Controller 全部返回 `Result<T>`。
  - 依赖：M0-T4
  - 优先级：P0

- **任务 M1-T2：定义 `PageResp<T>`**
  - 子任务：
    - 字段：`pageNo / pageSize / total / records`。
    - 与 MyBatis-Plus `IPage` 互转工具方法。
  - 验收：
    - [ ] 列表分页接口全部返回 `PageResp<T>` 形态。
  - 依赖：M1-T1
  - 优先级：P0

### M1-MOD-2 异常体系

- **任务 M1-T3：业务异常 `BizException`**
  - 子任务：
    - 构造参数支持 `ErrorCode` 与自定义消息。
  - 验收：
    - [ ] Service 抛出，业务异常不带堆栈。
  - 依赖：M1-T1
  - 优先级：P0

- **任务 M1-T4：错误码字典 `ErrorCode`**
  - 子任务：
    - 至少定义：`BIZ_PARAM_INVALID` / `BIZ_NOT_FOUND` / `BIZ_TRANSITION_NOT_ALLOWED` / `BIZ_P0_CLARIFICATION_BLOCKED` / `BIZ_DONE_IMMUTABLE` / `SYS_INTERNAL`。
    - 提供 code/中文 message 静态常量。
  - 验收：
    - [ ] 业务异常与系统异常错误码分离。
    - [ ] 文档（`docs/api/error-codes.md` 占位）登记每个错误码。
  - 依赖：M1-T1
  - 优先级：P0

- **任务 M1-T5：全局异常处理器 `GlobalExceptionHandler`**
  - 子任务：
    - 捕获 `BizException` → `Result.error(ErrorCode, msg)`。
    - 捕获 `MethodArgumentNotValidException` / `BindException` → `BIZ_PARAM_INVALID`。
    - 捕获 `Exception` → `SYS_INTERNAL`，记录 ERROR 日志。
  - 验收：
    - [ ] 错误信息不泄露堆栈与敏感字段。
    - [ ] 校验失败时返回字段级 message。
  - 依赖：M1-T3, M1-T4
  - 优先级：P0

### M1-MOD-3 用户上下文（最小化）

- **任务 M1-T6：请求头 `X-User` 解析**
  - 子任务：
    - `UserContext` ThreadLocal 持有 `userId / displayName`。
    - `UserContextFilter` 从 `X-User` 写入；缺省 `anonymous`。
  - 验收：
    - [ ] Service 拿不到 user 不影响流程（最小化）。
    - [ ] 与 §7 Q13 默认假设一致。
  - 依赖：M1-T5
  - 优先级：P2

---

## M2 · 数据模型与字典

> 目标：库表 + 字典一次性建好；M3~M6 在此之上编码。

### M2-MOD-1 数据库表

- **任务 M2-T1：核心 6 张表**
  - 子任务：
    - `work_item`：聚合根，字段见需求 §3.1。
    - `work_item_status_history`：状态历史。
    - `clarification_question`：澄清问题。
    - `ai_analysis_result`：AI 分析结果（`payload` JSON）。
    - `dict_item`：通用字典（key/type/label/value/sort/enabled）。
    - `user`（可选最小化）：仅 `id / username / display_name`。
  - 验收：
    - [ ] 字段类型与 §3 一致；`bigint` 主键；通用字段 `create_time / update_time / deleted` 落地。
    - [ ] 必要索引：`(status, priority)`、`clarification(work_item_id, severity, status)`、`ai_analysis(work_item_id, analysis_type)`。
  - 依赖：M0-T4
  - 优先级：P0

- **任务 M2-T2：`db.sql` 与种子数据**
  - 子任务：
    - 导出 `docs/database/db.sql`（含建表、索引、初始字典、演示数据）。
    - `docs/sample-data/work-items.seed.json` 关联到导入脚本。
  - 验收：
    - [ ] demo profile 启动后即可演示完整闭环。
  - 依赖：M2-T1
  - 优先级：P0

### M2-MOD-2 枚举（Java 端）

- **任务 M2-T3：业务枚举**
  - 子任务：
    - `WorkItemTypeEnum`：`STORY / BUG / TASK`。
    - `WorkItemPriorityEnum`：`P0 / P1 / P2 / P3`。
    - `WorkItemStatusEnum`：`DRAFT / ANALYZING / READY / IN_PROGRESS / IN_TESTING / DONE`。
    - `ClarificationSeverityEnum`：`P0 / P1 / P2`。
    - `ClarificationStatusEnum`：`OPEN / RESOLVED`。
    - `AiAnalysisTypeEnum`：`SUMMARY / ACCEPTANCE / RISK / CLARIFICATION / TASK_BREAKDOWN`。
    - `AiSourceEnum`：`MOCK / LLM`。
    - `RiskLevelEnum`（可选）：`LOW / MEDIUM / HIGH`。
  - 验收：
    - [ ] 枚举含 `code` / `desc`；`codeOf(...)` 静态方法存在。
  - 依赖：M2-T1
  - 优先级：P0

---

## M3 · 工作项模块（workitem）

> 第一个业务模块，落地“创建-查询-修改”主链路；为 M4/M5/M6 提供上下文。

### M3-MOD-1 Entity / Mapper

- **任务 M3-T1：`WorkItemEntity`**
  - 子任务：
    - 字段一一对应表结构；`@TableLogic` 软删除；`@Version` 乐观锁（加分项默认开启）。
    - 通用字段自动填充（`MetaObjectHandler`）。
  - 验收：
    - [ ] Entity 不出 Controller。
    - [ ] `@TableField` 命名正确（下划线转驼峰）。
  - 依赖：M2-T1, M2-T3
  - 优先级：P0

- **任务 M3-T2：`WorkItemMapper`**
  - 子任务：
    - 继承 `BaseMapper<WorkItemEntity>`。
    - 复杂查询使用 XML/注解 SQL，分页 `LambdaQueryWrapper` 优先。
  - 验收：
    - [ ] 无业务注解；纯数据访问。
  - 依赖：M3-T1
  - 优先级：P0

### M3-MOD-2 DTO / VO

- **任务 M3-T3：请求 DTO**
  - 子任务：
    - `WorkItemCreateReqDTO` / `WorkItemUpdateReqDTO` / `WorkItemQueryReqDTO`。
    - JSR-303 注解：`@NotBlank` / `@Size` / `@Min` 等。
  - 验收：
    - [ ] 校验失败 → `BIZ_PARAM_INVALID`。
  - 依赖：M3-T1
  - 优先级：P0

- **任务 M3-T4：响应 VO**
  - 子任务：
    - `WorkItemRespVO` / `WorkItemDetailRespVO`（含澄清问题计数、AI 分析次数）。
  - 验收：
    - [ ] Entity 不透出。
  - 依赖：M3-T1
  - 优先级：P0

### M3-MOD-3 Service

- **任务 M3-T5：`WorkItemService`**
  - 子任务：
    - `createWorkItem`：`DRAFT` 状态写入；事务内同时写状态历史。
    - `updateWorkItem`：除 `status` 外可改；`status` 走流转接口。
    - `getWorkItemDetail`：含聚合信息。
    - `pageWorkItem`：`status / type / priority / keyword` 过滤；显式排序。
  - 验收：
    - [ ] 跨多表写操作加 `@Transactional`。
    - [ ] Service 不返回 Entity。
  - 依赖：M3-T2, M3-T3, M3-T4
  - 优先级：P0

### M3-MOD-4 Controller

- **任务 M3-T6：`WorkItemController`**
  - 子任务：
    - `POST /api/v1/work-items` 创建。
    - `PUT /api/v1/work-items/{id}` 修改（不改状态）。
    - `GET /api/v1/work-items` 分页。
    - `GET /api/v1/work-items/{id}` 详情。
  - 验收：
    - [ ] 全返回 `Result<T>`。
    - [ ] 入参 DTO + `@Valid`。
  - 依赖：M3-T5
  - 优先级：P0

---

## M4 · 状态流转（状态机）

> 决定系统“规则最硬”的部分：邻接表 + 守卫链 + 历史。

### M4-MOD-1 邻接表与状态机引擎

- **任务 M4-T1：状态机白名单**
  - 子任务：
    - 邻接表定义（基于 §7 默认假设）：
      - `DRAFT → ANALYZING`
      - `ANALYZING → READY / DRAFT`（回退）
      - `READY → IN_PROGRESS / ANALYZING`（回退）
      - `IN_PROGRESS → IN_TESTING / READY`（回退）
      - `IN_TESTING → DONE / IN_PROGRESS`（回退）
      - `DONE → 无`（终态）
    - 提供 `Map<WorkItemStatusEnum, Set<WorkItemStatusEnum>>`。
  - 验收：
    - [ ] 邻接表集中在一处，单测覆盖每一跳。
  - 依赖：M3-T1
  - 优先级：P0

- **任务 M4-T2：`StateMachine`**
  - 子任务：
    - `canTransit(from, to)` / `assertTransit(from, to)`。
    - `DONE` 目标/源均拒绝（`BIZ_DONE_IMMUTABLE`）。
  - 验收：
    - [ ] 非法流转抛出 `BizException` + 明确错误码。
  - 依赖：M4-T1
  - 优先级：P0

### M4-MOD-2 业务守卫（链式）

- **任务 M4-T3：`WorkItemTransitionGuard` 接口**
  - 子任务：
    - 接口签名：`void check(WorkItemEntity current, WorkItemStatusEnum target)`。
  - 验收：
    - [ ] 多个实现按序执行，任一失败立即抛 `BizException`。
  - 依赖：M4-T2
  - 优先级：P0

- **任务 M4-T4：`P0ClarificationGuard`**
  - 子任务：
    - 若 `target ∈ {READY, IN_PROGRESS, IN_TESTING, DONE}` 且存在 `severity=P0 & status=OPEN` 的澄清问题 → 抛 `BIZ_P0_CLARIFICATION_BLOCKED`。
  - 验收：
    - [ ] 单测覆盖 4 个目标态。
    - [ ] 解决 P0 后放行。
  - 依赖：M4-T3, M5-T1（M5 落地后联调）
  - 优先级：P0

### M4-MOD-3 流转服务

- **任务 M4-T5：`WorkItemTransitionService`**
  - 子任务：
    - 加载工作项 → 执行守卫链 → 写新状态 + 状态历史（同事务）。
    - 记录 `from / to / reason / operator / time`。
  - 验收：
    - [ ] 写历史与状态变更在同一 `@Transactional`。
    - [ ] 历史接口 `GET /api/v1/work-items/{id}/transitions` 可查。
  - 依赖：M4-T2, M4-T4
  - 优先级：P0

- **任务 M4-T6：`WorkItemTransitionController`**
  - 子任务：
    - `POST /api/v1/work-items/{id}/transitions { targetStatus, reason }`。
  - 验收：
    - [ ] 返回新状态 + 最新历史记录。
  - 依赖：M4-T5
  - 优先级：P0

---

## M5 · 澄清问题（clarification）

> 让“未澄清项”可见、可跟踪，并被 M4 守卫消费。

### M5-MOD-1 Entity / Mapper / DTO / VO

- **任务 M5-T1：`ClarificationEntity` + Mapper**
  - 子任务：
    - 字段与 §3.3 一致。
    - 索引：`(work_item_id, severity, status)`。
  - 验收：
    - [ ] 单表 CRUD 通过。
  - 依赖：M2-T1, M2-T3
  - 优先级：P0

- **任务 M5-T2：DTO / VO**
  - 子任务：
    - `ClarificationCreateReqDTO` / `ClarificationResolveReqDTO` / `ClarificationRespVO`。
  - 验收：
    - [ ] 严重程度与状态必填。
  - 依赖：M5-T1
  - 优先级：P0

### M5-MOD-2 Service

- **任务 M5-T3：`ClarificationService`**
  - 子任务：
    - `addQuestion(workItemId, dto)`。
    - `resolveQuestion(id, dto)`：写入 `answer / resolvedBy / resolvedAt / status=RESOLVED`。
    - `listQuestions(workItemId, severity?, status?)`。
    - 统计 `P0 & OPEN` 数量（供 M4 守卫使用）。
  - 验收：
    - [ ] 解决时不修改 `workItem.status`（仅清守卫条件）。
    - [ ] Service 方法命名语义清晰。
  - 依赖：M5-T1, M5-T2
  - 优先级：P0

### M5-MOD-3 Controller

- **任务 M5-T4：`ClarificationController`**
  - 子任务：
    - `POST /api/v1/work-items/{id}/clarifications`。
    - `PUT /api/v1/clarifications/{id}`。
    - `GET /api/v1/work-items/{id}/clarifications`。
  - 验收：
    - [ ] 全 `Result<T>` 返回。
    - [ ] 联调通过 M4 守卫。
  - 依赖：M5-T3
  - 优先级：P0

---

## M6 · AI 辅助分析（ai）

> “工程化封装 AI” 是差异化能力。能力抽象 + 至少 3 种实现 + Mock/可替换。

### M6-MOD-1 能力抽象

- **任务 M6-T1：`AiCapability` 接口**
  - 子任务：
    - 方法：`AiAnalysisTypeEnum supports()` + `AiAnalysisPayload analyse(WorkItemEntity wi)`。
    - `AiAnalysisPayload` 用 Jackson `Map<String, Object>` 承载结构化结果。
  - 验收：
    - [ ] 强制结构化输出（不允许返回 String/散文）。
  - 依赖：M3-T1
  - 优先级：P0

- **任务 M6-T2：`MockAiAdapter` / `LlmAiAdapter`（可切换）**
  - 子任务：
    - `MockAiAdapter`：基于规则模板返回结构化 JSON（按 `analysisType` 切模板）。
    - `LlmAiAdapter`（加分）：通过 `WebClient` / `RestClient` 调用，prompt 模板外置。
    - 切换策略：`AiAdapterRouter` 按配置 `ai.source = mock | llm` 选择。
  - 验收：
    - [ ] 配置项 `ai.source` 切换后无需改业务代码。
    - [ ] Mock 输出符合固定 schema，schema 改动集中在一处。
  - 依赖：M6-T1
  - 优先级：P0（Mock）/ P2（LLM）

### M6-MOD-2 能力实现（至少 3 种）

- **任务 M6-T3：`SummaryCapability`**
  - 子任务：
    - 输出：`{ "summary": "...", "keyPoints": [...] }`。
  - 验收：
    - [ ] 返回结构化 Map，key 固定。
  - 依赖：M6-T1
  - 优先级：P0

- **任务 M6-T4：`RiskCapability`**
  - 子任务：
    - 输出：`{ "risks": [{"level":"LOW|MEDIUM|HIGH", "description":"..."}, ...] }`。
  - 验收：
    - [ ] 至少识别“需求不清/依赖缺失/性能风险/历史返工率”4 类中 2 类。
  - 依赖：M6-T1
  - 优先级：P0

- **任务 M6-T5：`ClarificationCapability`**
  - 子任务：
    - 输出：`{ "questions": [{"question":"...", "severity":"P0|P1|P2"}, ...] }`。
  - 验收：
    - [ ] 可与 M5 对接：触发后可选自动写入 `ClarificationQuestion`（开关控制）。
  - 依赖：M6-T1
  - 优先级：P0

- **任务 M6-T6：（可选）`AcceptanceCriteriaCapability` / `TaskBreakdownCapability`**
  - 子任务：
    - 输出验收标准 / 子任务清单。
  - 验收：
    - [ ] 至少 1 种可触发。
  - 依赖：M6-T1
  - 优先级：P2

### M6-MOD-3 调度与持久化

- **任务 M6-T7：`AiAnalysisService`**
  - 子任务：
    - `triggerAnalysis(workItemId, type)`：定位 `AiCapability` → 调用 Adapter → 解析 → 持久化。
    - `listAnalyses(workItemId, type?)`。
  - 验收：
    - [ ] 解析失败抛 `BIZ_PARAM_INVALID` 并记录 WARN。
    - [ ] 历史可查。
  - 依赖：M6-T2, M6-T3~T6
  - 优先级：P0

- **任务 M6-T8：`AiAnalysisController`**
  - 子任务：
    - `POST /api/v1/work-items/{id}/ai-analyses { analysisType }`。
    - `GET /api/v1/work-items/{id}/ai-analyses`。
  - 验收：
    - [ ] 触发后立刻返回结构化结果与历史摘要。
  - 依赖：M6-T7
  - 优先级：P0

---

## M7 · 字典管理（dict）

> 状态/类型/优先级/严重度可后台维护，便于后续扩展。

### M7-MOD-1 后台接口

- **任务 M7-T1：`DictItemEntity` / Mapper**
  - 子任务：
    - 字段：`type / key / label / value / sort / enabled`。
  - 验收：
    - [ ] 与 §3 M2 字典表一致。
  - 依赖：M2-T1
  - 优先级：P1

- **任务 M7-T2：`DictService` / `DictController`**
  - 子任务：
    - `GET /api/v1/dicts?type=...` 对外只读查询。
    - `POST /api/v1/dicts` / `PUT /api/v1/dicts/{id}` / `DELETE /api/v1/dicts/{id}` 管理（仅 admin 简化校验）。
  - 验收：
    - [ ] 字典变更不影响已存在工作项（兼容旧值）。
  - 依赖：M7-T1
  - 优先级：P1

- **任务 M7-T3：枚举 ↔ 字典映射**
  - 子任务：
    - 业务枚举优先从字典表取 `label`；字典缺失时回退到枚举内置。
  - 验收：
    - [ ] 单测覆盖“字典命中/未命中”两条路径。
  - 依赖：M7-T2, M2-T3
  - 优先级：P2

---

## M8 · 简单前端页面

> 最小化可演示闭环：列表、详情、流转、澄清、AI 触发。

### M8-MOD-1 页面骨架

- **任务 M8-T1：路由与布局**
  - 子任务：
    - `/` 工作项列表。
    - `/work-items/:id` 详情。
    - 顶部全局 `X-User` 输入框（演示用）。
  - 验收：
    - [ ] 路由跳转正常。
  - 依赖：M3-T6
  - 优先级：P1

- **任务 M8-T2：API 封装**
  - 子任务：
    - `src/api/workItem.js` / `clarification.js` / `ai.js` / `dict.js`。
    - 统一 `request` 实例：拦截 `Result.code !== 0`、loading、错误提示。
  - 验收：
    - [ ] 不在页面里硬编码请求地址。
  - 依赖：M3-T6, M5-T4, M6-T8
  - 优先级：P1

### M8-MOD-2 业务交互

- **任务 M8-T3：列表 + 详情**
  - 子任务：
    - 列表：分页、过滤、跳转详情。
    - 详情：基本信息、澄清问题列表、状态历史、AI 分析历史。
  - 验收：
    - [ ] 数据与后端一致。
  - 依赖：M8-T1, M8-T2
  - 优先级：P1

- **任务 M8-T4：状态流转弹窗**
  - 子任务：
    - 仅展示邻接表中的合法下一状态。
    - 阻断时显示具体原因（含 P0 澄清问题链接）。
  - 验收：
    - [ ] 阻断原因与 `BIZ_P0_CLARIFICATION_BLOCKED` 错误码一致。
  - 依赖：M8-T3, M4-T6
  - 优先级：P1

- **任务 M8-T5：澄清问题 CRUD**
  - 子任务：
    - 新增、解决、过滤（severity / status）。
  - 验收：
    - [ ] 操作后无需刷新即可看到守卫放行。
  - 依赖：M8-T3, M5-T4
  - 优先级：P1

- **任务 M8-T6：AI 分析触发与展示**
  - 子任务：
    - 选择 `analysisType` → 触发 → 渲染结构化 JSON（折叠/卡片化）。
  - 验收：
    - [ ] 看到的是结构化数据，不是文本段落。
  - 依赖：M8-T3, M6-T8
  - 优先级：P1

---

## M9 · 测试与质量

> 关键规则必须有单测，保证演示与重构不破窗。

### M9-MOD-1 状态机测试

- **任务 M9-T1：合法流转**
  - 子任务：
    - 覆盖 6 态 → 邻接表所有一跳。
  - 验收：
    - [ ] `mvn test` 全绿。
  - 依赖：M4-T2
  - 优先级：P0

- **任务 M9-T2：非法流转**
  - 子任务：
    - 覆盖：跳级、终态外任意变更、`DONE → *`。
  - 验收：
    - [ ] 错误码与 `BIZ_TRANSITION_NOT_ALLOWED` / `BIZ_DONE_IMMUTABLE` 一致。
  - 依赖：M4-T2
  - 优先级：P0

### M9-MOD-2 业务规则测试

- **任务 M9-T3：P0 拦截**
  - 子任务：
    - 4 个目标态各 1 个用例。
    - 解决 P0 后同一跳转 1 个用例。
  - 验收：
    - [ ] `BIZ_P0_CLARIFICATION_BLOCKED` 与 message 携带数量。
  - 依赖：M4-T4
  - 优先级：P0

- **任务 M9-T4：澄清问题 CRUD**
  - 子任务：
    - 新增/解决/查询/过滤；`P0` 计数实时更新。
  - 验收：
    - [ ] 单元 + 集成各 1 组用例。
  - 依赖：M5-T3
  - 优先级：P1

### M9-MOD-3 AI 适配层测试

- **任务 M9-T5：Mock 适配基本行为**
  - 子任务：
    - 3 种能力各 1 个用例，断言返回结构化字段齐全。
    - `AiAdapterRouter` 切换单测。
  - 验收：
    - [ ] 字段缺失立即失败。
  - 依赖：M6-T2
  - 优先级：P0

### M9-MOD-4 集成与冒烟

- **任务 M9-T6：端到端冒烟脚本**
  - 子任务：
    - 用 `MockMvc` / `RestAssured` 跑通：创建 → 流转被阻 → 解决 → 流转通过 → 触发 AI。
  - 验收：
    - [ ] 一键命令 `mvn -Psmoke verify` 全过。
  - 依赖：M3-T6, M4-T6, M5-T4, M6-T8
  - 优先级：P1

---

## M10 · 文档与交付

> 团队规范要求过程可追溯、AI 使用有记录、启动有文档。

### M10-MOD-1 启动与运行

- **任务 M10-T1：README**
  - 子任务：
    - 后端启动（dev / demo profile）。
    - 前端启动。
    - 演示闭环脚本（按需求 §4.5 顺序）。
  - 验收：
    - [ ] 新人按 README 可独立跑通闭环。
  - 依赖：M8-T6
  - 优先级：P0

- **任务 M10-T2：`docs/process.md`**
  - 子任务：
    - 记录实际开发顺序、决策点、与规范的偏差。
  - 验收：
    - [ ] 与 `docs/templates/process.md` 字段对齐。
  - 依赖：M10-T1
  - 优先级：P0

- **任务 M10-T3：`docs/ai-usage.md`**
  - 子任务：
    - 记录 AI 能力边界、prompt 模板、可替换点、Mock 行为。
  - 验收：
    - [ ] 与 `docs/templates/ai-usage.md` 字段对齐。
  - 依赖：M6-T7
  - 优先级：P0

### M10-MOD-2 API 与错误码

- **任务 M10-T4：API 文档**
  - 子任务：
    - springdoc-openapi 注解 + `docs/api/openapi.json` 导出。
    - 错误码字典 `docs/api/error-codes.md`。
  - 验收：
    - [ ] `/swagger-ui` 可访问并展示全部必做接口。
  - 依赖：M3-T6, M4-T6, M5-T4, M6-T8
  - 优先级：P2

---

## M11 · 加分项

> 在必做闭环跑通后再投入，避免挤压 P0。

| 任务 | 说明 | 依赖 | 优先级 |
|---|---|---|---|
| M11-T1 springdoc-openapi | 自动生成接口文档 | M3~M6 | P2 |
| M11-T2 乐观锁 | `@Version` + 重试机制 | M3-T1 | P2 |
| M11-T3 用户上下文 | `X-User` 解析到审计字段 | M1-T6 | P2 |
| M11-T4 Docker Compose | 一键起 MySQL + 后端 + 前端 | M0~M8 | P2 |
| M11-T5 看板雏形 | 状态分列、拖拽（MVP） | M8-T3 | P3 |
| M11-T6 审计日志 | 状态/澄清/AI 触发全留痕 | M4~M6 | P3 |
| M11-T7 边界与并发测试 | 重试、并发创建、批量澄清 | M9 | P3 |

---

## 1. 依赖关系总览

```text
M0 ─┬─► M1 ─┬─► M3 ─┬─► M4 ─► M5 ─► M9 ─► M10   （关键路径）
     │       │       │                       ▲
     │       │       ├─► M6 ─────────────────┤
     │       │       └─► M7 (P1, 并行)        │
     │       │                               │
     │       └─► M2 ──────────────────────────┘
     │
     └─► M8 (前端, 跟随 M3~M6 完成逐步接入)
                              │
                              └─► M11 (加分项, 闭环后投入)
```

**关键路径**：M0 → M1 → M2 → M3 → M4 → M5 → M9 → M10。
**可并行支线**：
- M6 与 M5 并行（AI 能力在 M4 完成后即可独立开发，不依赖澄清问题业务）。
- M7 在 M2 完成后即可开工。
- M8 在每个后端接口 Ready 后增量接入。
- M11 全程后置。

---

## 2. 优先级矩阵

| 优先级 | 含义 | 任务集合 |
|---|---|---|
| P0 | 必做，关键路径，不做则演示失败 | M0 全部、M1 全部、M2 全部、M3 全部、M4 全部、M5 全部、M6 核心（接口+Mock+3 种能力）、M9-T1/T2/T3/T5、M10-T1/T2/T3 |
| P1 | 必做但非关键路径 | M7 全部、M8 全部、M9-T4/T6 |
| P2 | 加分项 | M1-T6、M6-T2 LLM、M6-T6、M7-T3、M10-T4、M11-T1~T4 |
| P3 | 可选/锦上添花 | M11-T5/T6/T7 |

---

## 3. 验收口径（贯穿所有任务）

> 每条任务都按以下维度定义 Done，避免“感觉完成了”：

1. **功能正确**：happy path + 至少 1 个 error path 验证通过。
2. **编码规范**：与 `group_development_rule.md` 一致（分层、命名、注释、事务、日志、异常）。
3. **接口契约**：`Result<T>` 统一、错误码来自 `ErrorCode`、入参 `@Valid`、分页 `PageResp<T>`。
4. **测试**：核心规则有单测；改动影响既有测试时同步更新。
5. **可演示**：能在 demo profile 下被前端或 curl 走通。
6. **可追溯**：状态/澄清/AI 触发有历史或日志。

---

## 4. 风险与回退预案（与需求 §9 对应）

| 风险 | 触发条件 | 回退预案 |
|---|---|---|
| R2 业务规则边界不清 | M4 守卫链实现时出现“既要…又要…”冲突 | 退回到“仅 P0 拦截 4 态”，其余规则移到 M11 加分项 |
| R3 AI 工程化被字符串化 | Mock 输出不稳定或能力无法切换 | 强制 `AiCapability` 接口 + `AiAnalysisPayload` schema，PR 卡口 |
| R5 前端扩张 | 单页 > 3 个弹窗或 > 200 行 | 砍掉看板雏形、详情页拆 Tab 推迟 |
| R8 时间挤兑 | P0 任一任务未达验收 | 停止所有 P2/P3 工作，关闭 M11 |
| R10 错误码混乱 | 出现裸 `code: 500` 或自由文本 | `BizException` 强制 + PR 评审检查 |

---

## 5. 进入实施的前置条件（与需求 §10 对齐）

- [x] §7 Q1、Q2、Q3、Q4、Q5、Q6、Q10、Q11、Q12 已采用默认假设落地，可继续执行；如需调整，对应 M0~M6 任务内容同步变更。
- [x] 状态机白名单（M4-T1）已定稿。
- [x] AI 能力范围（M6-T3/T4/T5）已定稿为 3 种（Summary / Risk / Clarification）。
- [x] 数据库与 Mock 策略（M2 / M6-T2）已定稿。

> 后续若有确认结果与默认假设冲突，请按本文件 §1 依赖关系反向回溯，更新对应任务。
