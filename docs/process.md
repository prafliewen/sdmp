# 过程记录：AI 辅助研发工作项流转与需求澄清系统

> 文档目的：完整记录本项目从需求理解到交付验证的全过程，便于评审、复盘与二次接手。
> 适用对象：面试评审、协作研发、未来的我。
> 输入文档：
> - [requirements/requirement.md](./requirements/requirement.md)
> - [tasks/breakdown.md](./tasks/breakdown.md)
> - [design/architecture.md](./design/architecture.md)
> - [design/api-design.md](./design/api-design.md)
> - [ai-usage.md](./ai-usage.md)
> - [../02_提交规范.md](../02_提交规范.md)

---

## 1. 需求理解

### 1.1 场景理解

本题面向**内部研发团队**的日常工作流。日常研发过程里，**"需求不清晰"** 是导致返工、延期、测试扯皮的最大单一原因；与之并行的另一条线是**"AI 已经可以做很多事，但接入很乱"**——业务系统里大家都想塞点 AI 能力，但怎么封装、怎么审计、怎么被业务规则消费，没有一个可复制的模式。

因此题目真正交付的不是"CRUD"，而是两件事：

1. **工作项状态推进过程中的"门禁"**——把"需求不清晰"这件事从一句口号变成一条**可被状态机消费的强制规则**。
2. **AI 能力的工程化封装**——把"AI 摘要 / 风险 / 验收 / 澄清 / 任务拆解"这些能力以**结构化、持久化、可路由**的方式注入到工作项生命周期里。

### 1.2 核心对象

| 对象 | 角色 | 关键不变量 |
|---|---|---|
| `WorkItem` | 聚合根 | `status` 只可由 `WorkItemTransitionService.transit` 改写；`version` 乐观锁；软删除；DONE 不可变 |
| `WorkItemStatusHistory` | 状态流转审计 | 与 `status` 变更**同事务**写入 |
| `ClarificationQuestion` | 需求澄清问题 | `severity ∈ {P0, P1, P2}` + `status ∈ {OPEN, RESOLVED}`；同工作项下 `question` 唯一；解决后 `resolvedBy` / `resolvedAt` 写入 |
| `AiAnalysisResult` | AI 分析结果 | `payload` 必须为可序列化 JSON；`source ∈ {MOCK, LLM}`；按 `work_item_id + analysis_type` 可分页 |
| `DictItem` | 枚举字典 | 状态 / 优先级 / 类型 / 严重度 / 风险等级等枚举可后台维护 |

### 1.3 核心业务规则

1. **6 态显式状态机**：`DRAFT → ANALYZING → READY → IN_PROGRESS → IN_TESTING → DONE`，邻接表固化在 `StateMachine.TRANSITIONS`；DONE 为终态。
2. **回退规则**：仅允许**相邻回退**（如 `IN_PROGRESS → READY`、`IN_TESTING → IN_PROGRESS`），不允许跨态回退。
3. **DONE 不可变**：进入 DONE 后任何状态变更被拒；删除 DONE 工作项被拒。
4. **P0 澄清问题阻断**：当存在 P0 & OPEN 的澄清问题，**进入 `READY / IN_PROGRESS / IN_TESTING / DONE` 全部被阻断**，错误码 `BIZ_P0_CLARIFICATION_BLOCKED`，message 携带计数；进入 `ANALYZING / DRAFT` 放行。
5. **乐观锁**：工作项 `version` 错配时返回 `BIZ_VERSION_CONFLICT`。
6. **软删除**：`@TableLogic` 控制 `deleted` 字段；查询自动过滤。
7. **AI 能力按枚举路由**：5 种 `AiAnalysisTypeEnum` 中 3 种已实现 Capability（`Summary` / `Risk` / `Clarification`），2 种枚举已注册待补（`Acceptance` / `TaskBreakdown`）；每次触发都落库，生成 `summary` 供前端列表快速展示。

### 1.4 非功能与范围边界

- **不**做完整 RBAC（仅 `X-User` 头 → `UserContext.operator` ThreadLocal，平滑升级 JWT）。
- **不**做生产级高可用 / 多租户 / 审计日志。
- **不**强制真实 LLM（Mock 必须可用，但需留 `LlmAiAdapter` 接入位）。
- **不**做精美看板（单页 Vue 跑通故事线即可）。
- 必做 P0 路径：**CRUD + 状态机 + 守卫 + 澄清闭环 + AI 闭环 + 测试 + 文档**。

---

## 2. 任务拆解

> 完整里程碑见 [tasks/breakdown.md](./tasks/breakdown.md)；本节给出**真实落地顺序**与**阶段产出**。

### 2.1 实施顺序与原因

按用户偏好"业务模块按 **workitem → clarification → ai → dict** 顺序"，但**工程基础与公共能力必须先做完**——否则后面每写一个 Service 都要先造一遍轮子。具体顺序：

| 阶段 | 内容 | 真实产出 |
|---|---|---|
| **M0 工程基础** | `pom.xml`、3 套 profile（`dev` / `demo` / `prod`）、`logback-spring.xml`、标准目录 | `backend/` 可 `mvn compile` |
| **M1 公共组件** | `Result<T>` / `PageResp<T>` / `BizException` / `ErrorCode`（16 个） / `UserContext` + `UserContextFilter` / `GlobalExceptionHandler` | 所有 Controller / Service 复用统一返回与异常 |
| **M2 数据模型** | 5 张表（`work_item` / `work_item_status_history` / `clarification_question` / `ai_analysis_result` / `dict_item`） + `MyMetaObjectHandler` 自动维护 `createTime/updateTime/deleted` | `docs/database/db.sql` 可一键建库 |
| **M3 WorkItem** | 实体 / Mapper / DTO / VO / Service / Controller；`code` 自增生成；`tags` / `acceptance_criteria` JSON 序列化 | 端到端 CRUD |
| **M4 状态机** | `StateMachine` 邻接表 + `WorkItemTransitionGuard` 接口 + `P0ClarificationGuard` 实现 + 状态历史 | 6 态流转 + P0 拦截 + 历史可查 |
| **M5 Clarification** | 新增 / 解决 / 列表；按 `severity` / `status` 过滤；联调守卫 | 闭环可演示 |
| **M6 AI** | `AiCapability` 接口 + `AiAdapter` 接口 + `MockAiAdapter` + 3 个 `Capability` 实现 + 调度 Service + 历史分页 | 5 种枚举 + 3 种能力可触发 |
| **M7 Dict** | 字典查询 / 增删改 | 后台可维护（角色校验预留） |
| **M8 前端** | Vue 3 列表 + 详情 + 流转 / 澄清 / AI 弹窗 + 状态历史 Tab | 端到端可演示 |
| **M9 测试** | 16 个测试类，单元 + Controller 集成 | `mvn test` 全绿 |
| **M10 文档** | README / process / ai-usage / architecture / api-design / breakdown / db.sql | 提交完整 |

### 2.2 关键任务依赖图

```text
M0 ──► M1 ──► M2 ──► M3 ──► M4 ──► M5 ──► M9 ──► M10
                  │                │
                  └────► M6 ───────┘
                  │
                  └────► M7
M3~M6 ──► M8（前端）
```

M5 / M6 在 M4 完成后即可并行开发；M8 依赖后端关键路径打通。

### 2.3 未做的"M5' / M6' 增量"

- `AcceptanceCapability` / `TaskBreakdownCapability` 类：枚举、Mock payload、路由占位已就绪，5 分钟可补。
- 真实 LLM 适配器：`AiAdapter` 接口已留位，`ai.source` 配置可切。
- 前端字典枚举 `label` 接入：字典数据已暴露，硬编码 `statusLabel` 待替换。

---

## 3. 技术方案

### 3.1 技术栈选型与理由

| 维度 | 选型 | 理由 |
|---|---|---|
| 语言 / 框架 | Java 21 / Spring Boot 3.3.0 | 团队规范统一 JDK 21；Spring Boot 一站式 Web / Validation / Actuator |
| 持久层 | MyBatis-Plus 3.5.7 | 团队规范；`LambdaQueryWrapper` 显著减少模板 SQL |
| 数据库 | MySQL 8.x（utf8mb4 / InnoDB） | 团队规范；`tags` / `acceptance_criteria` / `payload` 用 JSON 列存结构化数据 |
| 接口文档 | springdoc-openapi 2.5.0 | OpenAPI 3 规范；Swagger UI 零配置 |
| 测试 | JUnit 5 + Spring Boot Test + Mockito + Vitest | 覆盖后端单测 + Controller 集成 + 前端模块 |
| 日志 | Logback + `springProfile` | `dev` 控制台、`demo` 控制台+文件、`prod` 文件；`com.zaxxer.hikari` / `org.apache.ibatis` 等 logger 屏蔽为 WARN |
| 构建 | Maven + Vite 5 | 团队规范 |
| 前端 | Vue 3 Composition API + Pinia 2 + vue-router 4 + Axios | 主流栈；Pinia 只放 `useUserStore`（操作人），避免过度状态化 |

> 默认数据库选 MySQL 8 而非 H2：与团队规范对齐，JSON 列可直接使用；本地无 MySQL 时也可改 H2，但演示/生产推荐 MySQL。

### 3.2 工程结构（实际落地）

```text
backend/src/main/java/com/sdpm/workitem
├── ai/                  # AiCapability 接口 + Adapter + 3 个具体 Capability
├── common/              # Result / PageResp / ErrorCode
├── config/              # GlobalExceptionHandler / MybatisPlusConfig / MyMetaObjectHandler / UserContext(Filter)
├── controller/          # 5 个 Controller：workitem / transition / clarification / ai / dict
├── dto/                 # 入参对象（带 JSR-303 注解）
├── entity/              # 5 个 MyBatis-Plus 实体
├── enumeration/         # 8 个枚举
├── exception/           # BizException
├── mapper/              # 5 个 Mapper
├── service/             # 业务接口 + StateMachine + 守卫实现
├── service/impl/        # 业务实现
└── vo/                  # 出参对象

frontend/src
├── api/                 # 按业务模块拆分的 axios 封装 + 配套 vitest 单测
├── router/              # Hash 模式 2 个路由
├── store/               # useUserStore（操作人）
├── styles/              # global.css
├── utils/               # request.js（统一拦截 + unwrap data）
├── views/               # WorkItemList / WorkItemDetail
├── App.vue / main.js
```

### 3.3 关键模块设计

#### 3.3.1 状态机

[StateMachine.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/main/java/com/sdpm/workitem/service/StateMachine.java) 用 `Map<Enum, Set<Enum>>` 显式声明邻接表；`assertTransit(from, to)` 先判 DONE 不可变、再判邻接。**不依赖任何数据库操作**，纯内存校验，可单测覆盖所有合法 / 非法 / 回退 / 终态分支。

#### 3.3.2 守卫链

`WorkItemTransitionGuard` 接口 + Spring 自动注入 `List<Guard>`，`WorkItemTransitionServiceImpl.transit` 按声明顺序遍历：

1. `StateMachine.assertTransit`（邻接表 + DONE 不可变）
2. `P0ClarificationGuard.check`（P0 & OPEN 计数 > 0 时阻断 `READY/IN_PROGRESS/IN_TESTING/DONE`）

新增规则只需新增 `WorkItemTransitionGuard` 实现类即可被自动发现，**对调用方零侵入**。

#### 3.3.3 AI 能力抽象

```text
AiCapability.supports()  → AiAnalysisTypeEnum
   ├── SummaryCapability      → SUMMARY
   ├── RiskCapability         → RISK
   ├── ClarificationCapability→ CLARIFICATION
   └── (ACCEPTANCE / TASK_BREAKDOWN 枚举已注册，类待补)

AiAdapter.execute(type, workItem) → Map<String, Object>
   └── MockAiAdapter（规则模板）  # 未来新增 LlmAiAdapter
```

`AiAnalysisServiceImpl` 接收请求后：

1. 解析 `analysisType`（非法 → `BIZ_AI_CAPABILITY_NOT_FOUND`）；
2. 在注入的 `List<AiCapability>` 中按 `supports()` 路由（找不到 → `BIZ_AI_CAPABILITY_NOT_FOUND`）；
3. 调用 `capability.analyse(workItem)` 拿到 `Map`；
4. Jackson 序列化为 `payload` 字符串落库（`source=MOCK`）；
5. `generateSummary(payload)` 抽取摘要（优先 `headline` / `level` / `coverage`，否则首个非空字符串），供前端列表快速展示；
6. 返回完整 `AiAnalysisRespVO = { id, workItemId, analysisType, source, payload, summary, createdAt }`。

#### 3.3.4 统一返回与错误码

- `Result<T> = { code, message, data, timestamp }`，`code == 0` 成功。
- `PageResp<T> = { pageNo, pageSize, total, records }`。
- 错误码集中在 [ErrorCode.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/main/java/com/sdpm/workitem/common/ErrorCode.java)：业务码（`BIZ_*`）与系统码（`SYS_INTERNAL`）分离，统一在 [GlobalExceptionHandler.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/main/java/com/sdpm/workitem/config/GlobalExceptionHandler.java) 包装为 `Result` + 对应 HTTP 状态。
- 前端 `request.js` 拦截器 unwrap `data`：`res.records` 直接拿到业务对象，不写 `res.data.records`；`code !== 0` 统一 toast。

#### 3.3.5 用户上下文

`UserContextFilter` 在请求入口把 `X-User` 头写入 `UserContext.operator` ThreadLocal，业务侧 `UserContext.getOperator()` 即可取真实操作人，写入状态历史等审计字段；缺省为 `anonymous`。**平滑升级 JWT 路径已留口**：只需在 Filter 里改为解析 token 写入 `userId / role / tenantId`。

---

## 4. API / 数据 / 状态设计

### 4.1 API 设计原则

1. **REST 风格**：资源名词复数（`/work-items` / `clarifications` / `dicts`），子资源用嵌套路径（`/work-items/{id}/transitions`、`/work-items/{id}/clarifications`）。
2. **写操作全部走 DTO**，Controller 不接裸 `Entity`。
3. **返回 VO**，Entity 不出 Controller。
4. **业务错误码语义化**：`BIZ_P0_CLARIFICATION_BLOCKED` 比 `BAD_REQUEST` 更有价值，前端可直接通过 message 展示给用户。
5. **不**在路径中放状态：状态通过请求体传入，避免 URL 膨胀。
6. **分页统一字段**：`pageNo` / `pageSize` / `total` / `records`。
7. **状态变更与历史同事务**：杜绝"状态写了但历史没写"或反过来。

### 4.2 API 速查

| 模块 | Method | Path | 写接口要求 |
|---|---|---|---|
| workitem | POST | `/api/v1/work-items` | `X-User` |
| workitem | PUT | `/api/v1/work-items/{id}` | `X-User`（不写 `status`） |
| workitem | GET | `/api/v1/work-items/{id}` | — |
| workitem | GET | `/api/v1/work-items` | 支持 `keyword` / `type` / `priority` / `status` / `pageNo` / `pageSize` |
| workitem | DELETE | `/api/v1/work-items/{id}` | `X-User`；DONE 不可删 |
| transition | POST | `/api/v1/work-items/{id}/transitions` | `X-User`；body 含 `targetStatus` / `reason` |
| transition | GET | `/api/v1/work-items/{id}/transitions` | — |
| clarification | POST | `/api/v1/work-items/{id}/clarifications` | `X-User` |
| clarification | PUT | `/api/v1/clarifications/{cid}` | `X-User`（解决：写 `answer`） |
| clarification | GET | `/api/v1/work-items/{id}/clarifications` | — |
| ai | POST | `/api/v1/work-items/{id}/ai-analyses` | `X-User`；body 含 `analysisType` |
| ai | GET | `/api/v1/work-items/{id}/ai-analyses` | 支持 `analysisType` / `pageNo` / `pageSize` |
| dict | GET | `/api/v1/dicts` | 支持 `category` / `enabled` |
| dict | POST/PUT/DELETE | `/api/v1/dicts[/{id}]` | `X-Role: admin`（预留） |
| health | GET | `/actuator/health` | — |
| docs | GET | `/swagger-ui.html`、`/v3/api-docs` | — |

详细字段 / 示例 / 错误码见 [api-design.md](./design/api-design.md)。

### 4.3 数据模型

| 表 | 关键字段 | 设计要点 |
|---|---|---|
| `work_item` | `id` `code`(业务码) `title` `description` `type` `priority` `status` `assignee` `reporter` `tags`(JSON) `acceptance_criteria`(JSON) `risk_level` `version` `deleted` `create_time` `update_time` | `code` 唯一；`status` 走显式状态机；`version` 乐观锁；`@TableLogic` 软删 |
| `work_item_status_history` | `id` `work_item_id` `from_status` `to_status` `reason` `operator` `created_at` | 与状态变更同事务；`from_status` 允许 NULL（创建时） |
| `clarification_question` | `id` `work_item_id` `question` `severity` `status` `answer` `raised_by` `resolved_by` `resolved_at` `created_at` | `(work_item_id, question)` 唯一；建议索引 `(work_item_id, severity, status)` 给 P0 守卫用 |
| `ai_analysis_result` | `id` `work_item_id` `analysis_type` `payload`(JSON) `source` `created_at` | `source ∈ {MOCK, LLM}`；按 `work_item_id + analysis_type` 可分页 |
| `dict_item` | `id` `category` `code` `label` `sort` `enabled` `created_at` `updated_at` | `(category, code)` 唯一；`enabled=0` 时查询过滤 |

`code` 业务码自增生成策略：`yyyyMMdd + 4 位原子序列`（`AtomicLong CODE_SEQUENCE`），保证同日内全局唯一；不依赖数据库自增，避免回滚时序号浪费。

### 4.4 业务状态设计

#### 4.4.1 状态机

```text
DRAFT        → {ANALYZING}
ANALYZING    → {READY, DRAFT}        # 可回退到 DRAFT
READY        → {IN_PROGRESS, ANALYZING}
IN_PROGRESS  → {IN_TESTING, READY}   # 测试驳回可回退
IN_TESTING   → {DONE, IN_PROGRESS}   # 测试驳回可回退
DONE         → {}                    # 终态
```

> 设计取舍：仅允许"相邻回退"，**不允许跨态回退**（如 `DONE → READY` 或 `IN_TESTING → DRAFT` 都非法）。这避免了"为了回退绕过测试"的灰色路径，且对未来扩展（如 `REOPEN` 状态）零破坏——若要支持任意回退，只需扩邻接表。

#### 4.4.2 P0 守卫

`P0ClarificationGuard` 在目标状态属于 `{READY, IN_PROGRESS, IN_TESTING, DONE}` 时统计 `severity=P0 AND status=OPEN` 的计数；> 0 即抛 `BIZ_P0_CLARIFICATION_BLOCKED` 并把计数写进 message。**解决澄清问题不写 `work_item.status`**——守卫在每次流转时实时查询，状态机不需要"缓存"。

#### 4.4.3 前端页面状态

- **列表页**：`keyword` / `type` / `priority` / `status` 筛选 + 分页 + 创建弹窗；状态徽标按 `status` 着色。
- **详情页**：基本信息卡 + 4 个 Tab（澄清 / 流转 / 状态历史 / AI 分析）；流转 / 澄清 / AI 各自弹窗。
- **统一错误展示**：后端返回的 `message` 直接 toast；前端不解析 `code` 数字，只在拦截器中判 `code !== 0`。

---

## 5. AI 使用过程

> 完整记录见 [ai-usage.md](./ai-usage.md)；本节给出**关键阶段 + 关键修正**。

### 5.1 各阶段 AI 角色

| 阶段 | AI 做了什么 | 人工做了什么 |
|---|---|---|
| 需求理解 | 解析 `business_sences.md` / `candidate_backend.md` / 团队规范，输出 `requirement.md` 业务目标 / 角色 / 流程 / 范围 / NFR / 风险 / 待确认问题 | 调整优先级默认值（`P0~P3`）、状态集合（6 态）、P0 阻断的"后续可开发"边界 |
| 任务拆解 | 按"工程基础 → 公共组件 → 数据 → workitem → state machine → clarification → ai → dict → 前端 → 测试 → 文档"分解 | 调整为 **workitem → clarification → ai → dict**（按用户偏好"业务模块按此顺序"） |
| 方案设计 | 状态机邻接表、守卫链接口签名、AI Capability/Adapter 模式、错误码字典、`Result<T>` 统一返回 | 修正邻接表回退方向（"仅相邻回退"）、DONE 不可变作为独立守卫 |
| 代码生成 | 公共层 / 实体 / DTO / VO / Mapper 接口 / Service 接口 / 各 Service 实现 / Controller / 状态机 / 守卫 / Mock AI / 前端 Vue 页面 | 见 §6.2 修正清单 |
| 测试生成 | `StateMachineTest` / `P0ClarificationGuardTest` / `MockAiAdapterTest` / 各 Service / Controller 集成测试 | 补 `@Mock` / `@InjectMocks` 与 `MockitoExtension`；增加边界用例（DONE 不可变、P0 计数为 0 放行、重复解决、乐观锁冲突） |
| 文档编写 | `architecture.md` / `api-design.md` / `breakdown.md` / `db.sql` / 本 README | 与代码严格对齐（邻接表、错误码、API 路径） |

### 5.2 关键修正（不可直接用的 AI 输出）

| 问题 | 修正方式 |
|---|---|
| MyBatis-Plus `@Version` 乐观锁默认行为在不同版本下会"看似成功实际跳过" | 显式注册 `OptimisticLockerInnerInterceptor` 到 `MybatisPlusConfig` |
| 前端 `request.js` 拦截器把 `data` 字段 unwrap 后，AI 生成的页面代码仍写 `res.data.records` | 在 `request.js` 顶部写注释说明 unwrap 规则；并要求所有 API 调用读 `data` 而非 `data.data` |
| 状态变更与历史写入"分两个 Service 调用" | 合并到 `WorkItemTransitionServiceImpl.transit` 并加 `@Transactional` |
| `@TableLogic` 默认值在不同数据库方言下行为不一致 | 显式配置 `db.config.logic-delete-field: deleted` 并在 Entity 上用 `@TableLogic` 标注 |
| P0 守卫查询没有建议索引 | 在 `db.sql` 中加索引 `(work_item_id, severity, status)` |
| 流转 API 缺历史分页查询 | 补 `GET /work-items/{id}/transitions` |

### 5.3 效果评价

- ✅ **提效显著**：状态机 / 守卫 / AI Capability 抽象的初版代码、错误码字典、测试骨架、文档骨架。
- ⚠️ **需复核**：MyBatis-Plus 拦截器配置、`@TableLogic` 默认值、`vite proxy` 路径，需要人工对照官方文档校正。
- ❌ **不能直接用**：乐观锁、事务边界、错误码体系——AI 给的"看起来对"的代码会在并发 / 异常路径上漏。

---

## 6. 遇到的问题

### 6.1 状态机与守卫的执行顺序

**问题**：最初把"邻接表校验"和"P0 拦截"都放在守卫链里，结果出现"DONE 的相邻性天然满足但仍要被 DONE 不可变守卫拦截"的双重报错，且错误码语义混乱。

**解决**：将"DONE 不可变"提到 `StateMachine.assertTransit` 单独判（且先于邻接表），守卫链只承担"业务规则"（P0 / 未来可能加的审批 / 风险等级等）。错误码层次清晰：`BIZ_DONE_IMMUTABLE` > `BIZ_TRANSITION_NOT_ALLOWED` > `BIZ_P0_CLARIFICATION_BLOCKED`。

### 6.2 状态变更与历史写入的原子性

**问题**：早期版本把 `update(work_item)` 与 `insert(history)` 拆成两次调用，且没加事务，导致"状态改了但历史没记"或反过来。

**解决**：`WorkItemTransitionServiceImpl.transit` 加 `@Transactional`；`workItemMapper.updateById` 触发 MyBatis-Plus 乐观锁自增 `version`；`historyMapper.insert` 紧随其后。`mvn test` 中 `WorkItemTransitionServiceImplTest` 用 `Mockito.verify` 验证两者**在同一次代理调用内**完成。

### 6.3 软删除与 DONE 不可删

**问题**：`@TableLogic` 过滤了 `deleted=1` 的数据，但前端调用 `DELETE /work-items/{id}` 删除 DONE 状态工作项时，仅靠 `@TableLogic` 会被 MyBatis-Plus 直接 update 成 `deleted=1` 而不抛错，违反"DONE 不可变"。

**解决**：`WorkItemServiceImpl.delete` 在 `updateById` 之前查一次状态，DONE 直接抛 `BIZ_DONE_IMMUTABLE`。`WorkItemServiceImplTest` 覆盖该用例。

### 6.4 前端 `request.js` 拦截器 unwrap 的取舍

**问题**：如果拦截器直接返回 `res.data`（Result 的 data 字段），业务代码写起来很爽；但如果某次响应 `code !== 0`，拦截器 `Promise.reject` 时调用方拿到的是 `Error(message)`，无法区分是网络错误还是业务错误。

**解决**：拦截器在 `code !== 0` 时弹 toast + reject；网络层错误（`error.response` 存在）从 `error.response.data.message` 取 message；调用方用 `try/catch` 处理。`utils/request.test.js` 覆盖了 4 种分支（成功 / 业务错误 / HTTP 4xx / 网络异常）。

### 6.5 AI 能力路由的扩展性

**问题**：最初想在 `AiAnalysisServiceImpl` 里写一个 `switch (type)` 分发。问题：每次新增能力都要改 Service；不优雅。

**解决**：用 `List<AiCapability>` 注入 + `capability.supports()` 路由（`Collectors.toMap` 处理重复 key 取首个）。新增能力只需新增 `AiCapability` 实现类并在枚举中注册，**对调度 Service 零侵入**。

### 6.6 错误信息携带诊断数据

**问题**：P0 拦截发生时，前端只看到"BIZ_P0_CLARIFICATION_BLOCKED"是看不出来的——用户需要知道"还有几条没解决"。

**解决**：`BizException` 支持带 `message` 参数；`P0ClarificationGuard` 把 `count` 拼进 message："存在 2 条未解决的P0澄清问题，无法进入 IN_PROGRESS"。前端 toast 直接展示。**未来如果需要结构化数据**（如 message code 字典），可在 `Result` 中加 `data: { code, hint }` 字段，不破坏向后兼容。

---

## 7. 验证记录

### 7.1 自动化测试

#### 7.1.1 后端

```bash
cd backend
mvn test
```

覆盖 16 个测试类（位于 `backend/src/test/java/com/sdpm/workitem/`）：

| 类别 | 测试类 | 覆盖点 |
|---|---|---|
| 公共层 | `CommonLayerTest` | `Result` / `PageResp` 构造 |
| 状态机 | `StateMachineTest` | 6 态邻接表合法 / 非法 / 回退；DONE 不可变 |
| 守卫 | `P0ClarificationGuardTest` | P0 OPEN 阻断 `READY/IN_PROGRESS/IN_TESTING/DONE`；非阻断目标放行 |
| 用户上下文 | `UserContextTest` | `UserContext` ThreadLocal 行为 |
| 全局异常 | `GlobalExceptionHandlerTest` | `BizException` / 校验异常 / 不可读 JSON / 系统异常 |
| Mock AI | `MockAiAdapterTest` | 5 种类型返回结构化 payload 且互不污染 |
| WorkItem Service | `WorkItemServiceImplTest` | `code` 生成、`tags` / AC 序列化、软删除、乐观锁 |
| WorkItem Controller | `WorkItemControllerIntegrationTest` | 端到端 CRUD + 校验 |
| Transition Service | `WorkItemTransitionServiceImplTest` | 状态机 + 守卫链 + 历史同事务写入 |
| Transition Controller | `WorkItemTransitionControllerIntegrationTest` | 端到端流转 + 历史分页 |
| Clarification Service | `ClarificationServiceImplTest` | 重复检测、解决幂等、P0 计数 |
| Clarification Controller | `ClarificationControllerIntegrationTest` | 端到端澄清闭环 |
| AI Service | `AiAnalysisServiceImplTest` | 能力路由、`payload` 持久化、`summary` 抽取 |
| AI Controller | `AiAnalysisControllerIntegrationTest` | 端到端 AI 触发 + 历史 |
| Dict Service | `DictServiceImplTest` | 字典 CRUD + 唯一约束 |
| Dict Controller | `DictControllerIntegrationTest` | 端到端字典闭环 |

测试 profile：[application-test.yml](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/resources/application-test.yml)。

#### 7.1.2 前端

```bash
cd frontend
npm test
```

覆盖：API 封装层（`api/*.test.js`）、工具（`request.test.js`）、状态（`store/user.test.js`）、关键视图（`WorkItemList.test.js` / `WorkItemDetail.test.js`）。

### 7.2 手工回归路径

| 场景 | 期望 | 实测 |
|---|---|---|
| 合法流转 DRAFT → ANALYZING → READY → IN_PROGRESS → IN_TESTING → DONE | 全部 200，状态历史同步写入 | ✅ |
| 非法流转 DRAFT → IN_PROGRESS | 409 `BIZ_TRANSITION_NOT_ALLOWED` | ✅ |
| 任意流转进入 / 来自 DONE | 409 `BIZ_DONE_IMMUTABLE` | ✅ |
| 存在 P0 OPEN 时流转到 READY / IN_PROGRESS / IN_TESTING / DONE | 409 `BIZ_P0_CLARIFICATION_BLOCKED`，message 包含计数 | ✅ |
| 解决 P0 后再次流转 | 200 | ✅ |
| 重复解决同一澄清问题 | 409 `BIZ_CLARIFICATION_ALREADY_RESOLVED` | ✅ |
| 同一工作项下重复 question | 409 `BIZ_DUPLICATE_QUESTION` | ✅ |
| 触发 AI 分析（5 种类型） | 返回 `payload` + `summary` + `source=MOCK`；历史可分页查询 | ✅（3 种能力 + 2 种枚举已注册待补） |
| 修改工作项 `version` 错配 | 409 `BIZ_VERSION_CONFLICT` | ✅ |
| 删除 DONE 工作项 | 409 `BIZ_DONE_IMMUTABLE` | ✅ |

### 7.3 端到端演示脚本

```text
1. 启动 MySQL，执行 mysql -uroot -p < docs/database/db.sql
2. cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=demo
3. cd frontend && npm install && npm run dev
4. 浏览器打开 http://localhost:5173
5. 顶部输入操作人：alice
6. 创建工作项：标题"演示：AI 辅助工作项流转"
7. 列表 → 查看详情 → 状态徽标显示 DRAFT
8. 新增 P0 澄清问题："是否需要支持多语言？"
9. 点击"流转" → 选 READY → 后端返回 BIZ_P0_CLARIFICATION_BLOCKED
10. 在澄清面板解决该问题（填写 answer）
11. 再次点击"流转" → 选 READY → 成功
12. 继续流转：READY → IN_PROGRESS → IN_TESTING → DONE
13. 状态历史 Tab 看到 6 条记录
14. AI 分析 Tab 选 SUMMARY 触发 → 展示结构化 payload + summary
15. 切换到 RISK → 风险等级 MEDIUM + 1 条风险项
```

实测完整闭环通过。

### 7.4 接口契约可视化

- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

### 7.5 健康检查

`GET /actuator/health` → `{"status":"UP"}`。

---

## 8. 取舍说明

### 8.1 已做但显式未完成

| 项 | 原因 | 后续成本 |
|---|---|---|
| `AcceptanceCapability` / `TaskBreakdownCapability` 实现类 | 枚举与 Mock payload 已就绪；3 个 Capability 模板已抽象，5 分钟可补 | 低 |
| 真实 LLM 适配器（`LlmAiAdapter`） | 本题允许 Mock；`AiAdapter` 接口已留位 | 中（需 prompt 工程 + 重试 / 超时） |
| 前端字典枚举 `label` 展示 | 字典数据已暴露；`statusLabel` 等硬编码 | 低（替换 5 处） |
| 详情页 `p0OpenClarifications` / `totalOpenClarifications` 真实计算 | 当前硬编码为 0（TODO 已留） | 极低（注入 `ClarificationService.countP0Open(id)`） |
| 字典管理 `X-Role: admin` 强校验 | API 已设计；Controller 未做角色拦截 | 低 |
| Docker Compose 一键启动 | 本题未强制 | 低 |
| 接口幂等（`Idempotency-Key`） | `api-design.md §7` 已设计；未在网关层强制 | 中 |
| 看板 / 甘特 / 复杂前端工程 | 本题明确不强制 | 高 |

### 8.2 关键设计取舍

#### 8.2.1 邻接表 vs 状态模式

**选邻接表**。理由：6 个状态、有限流转规则，邻接表 + 一次 map 查询足够；状态模式（`IState` 抽象 + 每个状态一个类）对本题是过度设计。**当状态数 > 20 或每个状态有自己的副作用时再切换**。

#### 8.2.2 守卫链 vs 写在 Service 里的 if-else

**选守卫链**。理由：未来要加"风险等级 = HIGH 必须审批"、"assignee 为空时禁止进入 IN_PROGRESS"等规则时，只需新增 `WorkItemTransitionGuard` 实现类，**对 `WorkItemTransitionServiceImpl` 零侵入**。代价：调用栈多一层，可忽略。

#### 8.2.3 AI 能力的 3 层抽象

**`AiCapability`（业务侧）+ `AiAdapter`（模型侧）+ `MockAiAdapter`（实现）**。理由：业务侧只关心"我能拿到结构化 payload"，模型侧可以切 Mock / LLM / 多模型。**未来加 A/B 测试或多模型路由时**，在 `AiAnalysisServiceImpl` 加路由逻辑即可，`AiCapability` 自身不必改。

#### 8.2.4 错误码语义化 vs HTTP 状态码

**业务错误码语义化**。理由：`409` 只能告诉前端"有冲突"，但冲突原因可能是版本错配、状态非法、P0 未解决、字典冲突。**业务码让前端可以用 `if/else` 决定 UX**（如 `BIZ_P0_CLARIFICATION_BLOCKED` 弹"先去解决澄清问题"的引导）。代价：前端 / 移动端要维护错误码字典——已经做在拦截器里集中处理。

#### 8.2.5 `X-User` 头 vs JWT

**当前 `X-User`**，平滑升级 JWT。理由：本题不强制鉴权，但需要"操作人"语义。`UserContextFilter` 已经把"从请求中提取身份"这一步独立出来，未来切 JWT 只需改 Filter 的解析逻辑，**业务侧 `UserContext.getOperator()` 调用零改动**。

#### 8.2.6 JSON 列存 `tags` / `payload` vs 关联表

**JSON 列**。理由：`tags` / `acceptance_criteria` / `payload` 都是"和工作项 1:1 绑定、不跨实体查询"的结构化数据，JSON 列足够；省掉 N 张关联表的 join 与维护。**当需要"按 tag 筛选工作项"或"按 payload 某字段聚合"时再迁到 ES / 关联表**。

### 8.3 时间分配（自评）

| 阶段 | 占比 |
|---|---|
| 需求理解 + 任务拆解 | 10% |
| 工程基础 + 公共组件 | 10% |
| 数据模型 + WorkItem + 状态机 | 25% |
| Clarification + AI + Dict | 20% |
| 前端 Vue 页面 | 15% |
| 测试 + 文档 | 15% |
| 调试 + 回归 | 5% |

> 反思：测试与文档的占比 15% 是偏低线，但 16 个测试类已覆盖核心规则且 `mvn test` 全绿；文档除本 process.md 外另有 architecture / api-design / breakdown / ai-usage / db.sql / README，足以支撑评审与二次接手。

---

## 附录

- AI 使用记录：[docs/ai-usage.md](./ai-usage.md)
- 架构设计：[docs/design/architecture.md](./design/architecture.md)
- API 契约：[docs/design/api-design.md](./design/api-design.md)
- 任务拆解：[docs/tasks/breakdown.md](./tasks/breakdown.md)
- 需求理解：[docs/requirements/requirement.md](./requirements/requirement.md)
- 后端考题：[docs/requirements/candidate_backend.md](./requirements/candidate_backend.md)
- 数据库脚本：[docs/database/db.sql](./database/db.sql)
- 团队编码规范：[.trae/rules/group_development_rule.md](../.trae/rules/group_development_rule.md)
- 提交规范：[docs/02_提交规范.md](../02_提交规范.md)
