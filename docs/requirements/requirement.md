# 需求理解说明：AI 辅助研发工作项流转与需求澄清系统

> 文档目的：在编码开始前对齐对本项目业务目标、角色、流程、范围、非功能要求、风险、待确认问题与验收标准的理解。
> 适用对象：项目负责人、研发、测试、本次候选实现者。
> 输入文档：
> - [business_sences.md](./business_sences.md)（通用业务场景）
> - [candidate_backend.md](./candidate_backend.md)（后端方向考题）
> - [../../.trae/rules/group_development_rule.md](../../.trae/rules/group_development_rule.md)（团队编码规范）
> - [../process.md](../process.md)、[../ai-usage.md](../ai-usage.md)、[../templates/*](..//templates/)（过程/AI/测试/API 文档模板）
> - [../sample-data/work-items.seed.json](../sample-data/work-items.seed.json)（示例数据）

---

## 1. 业务目标

### 1.1 顶层目标

为内部研发团队提供一个轻量级、AI 辅助的工作项流转与需求澄清系统，覆盖研发过程中"需求不清晰 → 状态推进 → 完成验收"的关键链路，并沉淀"工程化封装 AI 能力"的最佳实践。

### 1.2 关键目标拆解

| 序号 | 业务目标 | 验收价值 |
|---|---|---|
| G1 | 工作项（需求/缺陷/任务）全生命周期管理 | 研发团队在一个入口完成工作项创建、维护、跟踪 |
| G2 | 明确的状态流转与业务规则 | 避免"需求未澄清就进入开发"，降低返工成本 |
| G3 | 需求澄清问题闭环 | 让"未澄清项"可见、可跟踪、可被状态机消费 |
| G4 | AI 能力以服务化、结构化方式注入流程 | 摘要、风险点、验收建议、澄清问题可被前端展示与后端规则使用 |
| G5 | 端到端可演示闭环 | 候选人可在一页内跑通核心故事线，证明系统可用 |
| G6 | 编码规范与过程可追溯 | 评审、协作、扩展有据可依，符合团队规范 |

### 1.3 非业务目标（明确不做）

- 不做完整企业级权限体系（仅做最小用户上下文即可）。
- 不做精美前端看板（仅做最小可演示页面）。
- 不强制真实调用 LLM（可 Mock，但需可替换为真实 AI 服务）。
- 不要求生产级安全与高可用。

---

## 2. 角色与利益相关方

| 角色 | 系统内称谓 | 核心诉求 | 关键交互 |
|---|---|---|---|
| 研发负责人 / Product Owner | `assignee` / `owner` | 跟踪整体进度、确认需求清晰度、把控质量门禁 | 创建工作项、查看状态历史、确认流转 |
| 研发工程师 | `assignee` | 接收任务、报告风险、推进状态 | 执行流转、登记澄清问题、调用 AI 分析 |
| 测试工程师 | `tester`（最小化） | 跟踪测试中、已完成状态、补充验收标准 | 查看状态、参与完成判定 |
| 产品 / 业务方 | `reporter`（隐含） | 提需求、回答澄清问题 | 提澄清问题答案 |
| AI 辅助服务 | `ai-service`（内部组件） | 接收工作项内容，输出结构化分析 | 摘要、风险、验收建议、澄清问题 |
| 系统管理员 | `admin`（可选） | 维护状态字典、配置 AI 接入 | 字典管理、配置切换 |

> 说明：本题重点考察后端工程能力，前端可简化，角色可由"单一用户 + 字段区分"代替完整 RBAC。

---

## 3. 核心概念与领域对象

### 3.1 工作项（WorkItem）

聚合根。表示一条研发任务（Story / Bug / Task）。

最小属性：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long / String | 主键 |
| title | String | 标题 |
| description | Text | 详细描述 |
| type | Enum | `STORY` / `BUG` / `TASK` |
| priority | Enum | `P0` / `P1` / `P2` / `P3`（待确认，见 §7） |
| status | Enum | 状态机当前状态 |
| assignee | String | 负责人 |
| tags | List<String> | 标签 |
| acceptanceCriteria | List<String> | 验收标准 |
| riskLevel | Enum | `LOW` / `MEDIUM` / `HIGH`（可选，待确认） |
| createdAt / updatedAt | DateTime | 通用字段 |
| version | Long | 乐观锁（加分项） |

子实体：

- `WorkItemStatusHistory`：状态流转历史。
- `ClarificationQuestion`：澄清问题列表（强关联工作项）。
- `AiAnalysisResult`：AI 分析结果（每次分析一条记录，可选）。

### 3.2 状态机

候选状态（基于业务场景要求"草稿→待分析→已准备→开发中→测试中→已完成"含义）：

```
DRAFT → ANALYZING → READY → IN_PROGRESS → IN_TESTING → DONE
                  ↑            ↓             ↓
                  └──(回退)─────┴─────────────┘
```

待确认点（见 §7）：

- 是否允许 `DONE` 状态变更？
- 是否允许"任意回退"或仅"按相邻步回退"？
- "已解决澄清问题"后是否需要重新走 `READY` 校验？

### 3.3 澄清问题（ClarificationQuestion）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| workItemId | Long | 所属工作项 |
| question | String | 问题内容 |
| severity | Enum | `P0` / `P1` / `P2`（高/中/低） |
| status | Enum | `OPEN` / `RESOLVED` |
| answer | String | 解决时的回答 |
| raisedBy | String | 提出人 |
| resolvedBy | String | 解决人 |
| createdAt / resolvedAt | DateTime | 时间字段 |

核心规则：

> 若工作项存在 `severity = P0` 且 `status = OPEN` 的澄清问题，则不允许进入 `READY` 及之后任何状态。

### 3.4 AI 分析结果（AiAnalysisResult）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 主键 |
| workItemId | Long | 所属工作项 |
| analysisType | Enum | `SUMMARY` / `ACCEPTANCE` / `RISK` / `CLARIFICATION` / `TASK_BREAKDOWN` |
| payload | JSON | 结构化结果（key-value 列表） |
| source | Enum | `MOCK` / `LLM`（区分真实/Mock 便于审计） |
| createdAt | DateTime | 生成时间 |

> 重点：结果必须"结构化返回"，禁止"一段散文"。

---

## 4. 业务流程

### 4.1 主流程（端到端闭环）

```text
创建工作项（DRAFT）
    ↓
进入待分析（ANALYZING）
    ↓
添加澄清问题（可多人/AI 辅助）
    ↓
尝试进入 READY ──【规则拦截：存在 P0 未解决澄清问题】──× 阻断，返回明确错误
    ↓（解决/调整 P0 澄清问题）
进入 READY
    ↓
进入 IN_PROGRESS → IN_TESTING → DONE
    ↓
触发 AI 分析 → 展示结构化结果（摘要/风险/澄清/任务拆解）
```

### 4.2 状态流转子流程

1. 接收流转请求 `POST /work-items/{id}/transitions { targetStatus, reason }`。
2. 加载当前工作项（含澄清问题计数）。
3. 校验合法性：
   - 是否在白名单流转图内？
   - 目标状态是否需要前置条件（如澄清问题拦截）？
   - 若是 `DONE`，是否允许再次变更？
4. 写入新状态 + 记录状态历史（同事务）。
5. 返回新状态与历史记录。

### 4.3 澄清问题子流程

1. 新增：`POST /work-items/{id}/clarifications`。
2. 解决：`PUT /clarifications/{id} { answer, status: RESOLVED }`。
3. 查询：`GET /work-items/{id}/clarifications`（支持按 severity/status 过滤）。
4. 影响流转：流转校验时实时统计 `P0 & OPEN` 数量。

### 4.4 AI 辅助子流程

1. 触发：`POST /work-items/{id}/ai-analyses { analysisType }`。
2. 服务定位：`AiAnalysisService` → 根据 `analysisType` 调度 `AiCapability`。
3. 执行：调用 `MockAiAdapter` 或 `LlmAiAdapter`（按配置切换）。
4. 解析：将文本/JSON 解析为结构化 `payload`。
5. 持久化：保存 `AiAnalysisResult`，返回结构化结果与历史。

### 4.5 演示闭环

```text
打开简单前端页面
  → 创建或查看工作项
  → 添加澄清问题（P0）
  → 尝试状态流转被阻断
  → 解决澄清问题
  → 状态流转成功
  → 触发 AI 分析
  → 展示结构化分析结果
```

---

## 5. 范围

### 5.1 必做（In Scope）

| 模块 | 关键能力 |
|---|---|
| 工作项管理 | 创建、查询（列表/详情）、修改、软删除（可选） |
| 状态流转 | 状态机定义、合法/非法校验、流转历史、错误返回 |
| 澄清问题 | 新增、解决、查询、严重程度、解决状态 |
| 核心业务规则 | P0 未解决澄清问题阻断后续状态 |
| AI 辅助分析 | 至少 1 种能力（推荐 3 种：摘要、风险、澄清问题），结构化返回，Mock/真实可切换 |
| 简单前端页面 | 列表/详情、触发流转、查看/新增澄清问题、触发 AI 分析并展示 |
| 测试 | 核心业务规则单测（合法流转、非法流转、规则阻断、AI 基本行为） |

### 5.2 加分（Nice to Have）

- OpenAPI / Swagger 文档。
- 状态流转历史查询 API。
- 乐观锁（@Version）并发更新保护。
- 统一错误码体系。
- 简单认证或用户上下文（请求头传 user）。
- Docker Compose 一键启动。
- 更完整前端（看板雏形、详情页、状态时间线）。
- 更完整测试覆盖（边界、并发、异常）。
- AI 过程文档与开发记录。

### 5.3 不做（Out of Scope）

- 完整企业级 RBAC。
- 看板拖拽、复杂前端工程。
- 真实 LLM 强制接入。
- 完整审计日志与数据权限隔离。
- 多租户。

---

## 6. 非功能要求

### 6.1 技术栈（结合团队规范）

| 维度 | 选择 | 说明 |
|---|---|---|
| 后端语言 | Java 21 | 团队规范 |
| 框架 | Spring Boot 3.x | 团队规范 |
| 持久层 | MyBatis-Plus | 团队规范 |
| 数据库 | MySQL 8 | 团队规范 |
| 构建 | Maven / Gradle | 团队规范 |
| 前后端 | Vue（简单页面） | 团队规范；本题可最小化 |
| 测试 | JUnit 5 + Mockito | 团队规范；可补充 Spring Boot Test |
| 文档 | springdoc-openapi（加分） | 团队规范推荐 OpenAPI |

### 6.2 API 规范

- REST 风格：`/api/v1/work-items`、`/api/v1/work-items/{id}/transitions`。
- 统一返回体 `Result<T>`：`code / message / data / timestamp`。
- 分页规范：`pageNo / pageSize / total / records`。
- 错误码：业务错误码（`BIZ_xxx`）与系统错误码（`SYS_xxx`）分离。
- 参数校验：DTO + JSR-303 注解。

### 6.3 编码规范（与团队规范对齐）

- 分层：Controller → Service → Mapper；Entity / DTO / VO 严格分离。
- 命名：类 UpperCamelCase，方法/变量 lowerCamelCase，常量全大写下划线。
- 注释：写"为什么"，不写"做什么"。
- 事务：跨多表操作必须 `@Transactional`。
- 日志：关键业务 INFO、可预期异常 WARN、系统异常 ERROR，禁止打印敏感字段。

### 6.4 质量要求

- 核心业务规则必须有单测。
- 关键接口（流转、澄清、AI）至少有 happy path + 1 个 error path 用例。
- Lint / 编译必须通过。

### 6.5 安全与合规

- 所有写接口必须做参数校验。
- 错误信息不泄露堆栈与敏感数据。
- 日志不打印密码、token、身份证。
- AI 适配层不直接拼接 prompt 到 SQL / 文件系统。

### 6.6 可扩展性

- AI 能力以 `AiCapability` 接口 + 多种 `Adapter` 实现，支持 Mock ↔ LLM 平滑切换。
- 状态机以"配置 + 引擎"或"显式 if/else 表"组织，便于后续增加状态。
- 业务规则以独立 `WorkItemTransitionGuard` 链式校验，便于新增规则。

---

## 7. 待确认问题（进入编码前需对齐）

> 以下问题影响状态机、业务规则与数据模型设计，建议在编码前与产品/面试官确认。

| 编号 | 问题 | 默认假设（若不澄清） | 影响范围 |
|---|---|---|---|
| Q1 | 状态集合是否必须严格使用"DRAFT/ANALYZING/READY/IN_PROGRESS/IN_TESTING/DONE"这 6 个？ | 是，使用 6 态 | 状态机与字典 |
| Q2 | 是否允许从 `DONE` 再变更为其他状态？ | 否（仅允许保持 `DONE`） | 流转规则 |
| Q3 | 是否允许任意相邻/非相邻状态回退（如 `IN_TESTING → ANALYZING`）？ | 仅允许回退到上一相邻状态 | 流转规则 |
| Q4 | "高优先级澄清问题"是指 `severity = P0`？还是自定义？ | `severity = P0` | 业务规则 |
| Q5 | "后续可开发状态"是指 `READY` 及之后？ | 是 | 业务规则 |
| Q6 | 解决 P0 澄清问题后是否需要重新校验 `READY` 前置条件？ | 是（实时校验） | 业务规则 |
| Q7 | 优先级枚举是 `HIGH/MEDIUM/LOW` 还是 `P0/P1/P2/P3`？ | `P0/P1/P2/P3` | 数据模型 |
| Q8 | 工作项 `type` 枚举除 `STORY/BUG/TASK` 外是否需要 `EPIC`？ | 不需要 | 数据模型 |
| Q9 | 数据库默认用 MySQL？ | MySQL 8 | 工程配置 |
| Q10 | AI 能力优先实现哪几种？ | 摘要 + 风险 + 澄清问题（3 种） | AI 模块 |
| Q11 | AI 必须真实可调用还是 Mock 即可？ | Mock 即可，需展示可替换为真实 LLM | AI 模块 |
| Q12 | 前端是否必须独立 Vue 工程？ | 允许最小化 HTML/JS 单页 | 前端 |
| Q13 | 是否需要用户登录？ | 不需要，请求头传 `X-User` 即可 | 鉴权 |
| Q14 | 状态历史是否必须可查询？ | 是 | API 设计 |

---

## 8. 验收标准（按模块拆解）

### 8.1 工作项管理

- [ ] `POST /api/v1/work-items`：可创建工作项，校验必填字段。
- [ ] `GET /api/v1/work-items`：分页查询，支持 `status / type / priority / keyword` 过滤。
- [ ] `GET /api/v1/work-items/{id}`：返回工作项详情（含澄清问题计数、AI 分析次数）。
- [ ] `PUT /api/v1/work-items/{id}`：可修改除 `status` 外的字段；`status` 必须走流转接口。
- [ ] 统一返回 `Result<T>` 格式。

### 8.2 状态流转

- [ ] 合法流转可成功并写入历史。
- [ ] 非法流转返回明确错误（如 `BIZ_TRANSITION_NOT_ALLOWED`）。
- [ ] 存在 `P0 & OPEN` 澄清问题时，进入 `READY/IN_PROGRESS/IN_TESTING/DONE` 全部被阻断。
- [ ] 解决 P0 问题后可流转通过。
- [ ] 状态历史可查询（`GET /api/v1/work-items/{id}/transitions`）。
- [ ] `DONE` 状态再次变更被拒绝。

### 8.3 澄清问题

- [ ] `POST /api/v1/work-items/{id}/clarifications` 新增问题。
- [ ] `PUT /api/v1/clarifications/{id}` 解决问题。
- [ ] `GET /api/v1/work-items/{id}/clarifications` 查询。
- [ ] 严重程度与状态字段必填且校验。
- [ ] P0 未解决问题数量会被状态机消费。

### 8.4 AI 辅助能力

- [ ] 至少 1 种（推荐 3 种）AI 能力可触发。
- [ ] 触发后返回**结构化**结果（不是一段散文）。
- [ ] 通过 `AiCapability` 接口注入，Mock 与 LLM 可切换。
- [ ] 触发记录持久化，详情页可查看历史结果。

### 8.5 简单前端页面

- [ ] 可看到工作项列表与详情。
- [ ] 可点击"流转"按钮并展示结果（成功 / 阻断原因）。
- [ ] 可新增/查看澄清问题。
- [ ] 可触发 AI 分析并展示结构化结果。
- [ ] 页面与后端打通，刷新数据一致。

### 8.6 测试

- [ ] 状态机合法流转单测。
- [ ] 状态机非法流转单测。
- [ ] 业务规则（P0 阻断）单测。
- [ ] AI 适配层基本行为单测（Mock 返回结构化结果）。
- [ ] `mvn test` 或等价命令全部通过。

### 8.7 工程与文档

- [ ] 项目结构符合团队规范（`controller/service/mapper/dto/vo/entity`）。
- [ ] `application.yml` 提供 dev / demo 配置。
- [ ] 启动文档清晰（如何启动后端与前端）。
- [ ] `process.md` / `ai-usage.md` 填写完整。
- [ ] 代码无调试输出与硬编码敏感信息。

---

## 9. 风险与缓解

| 编号 | 风险 | 影响 | 缓解策略 |
|---|---|---|---|
| R1 | 状态机设计过细导致返工 | 中 | 先用最小 6 态 + 邻接表跑通，规则集中在一处 |
| R2 | 业务规则（澄清问题阻断）边界不清 | 高 | §7 Q4-Q6 提前对齐，默认假设写在文档中 |
| R3 | AI 能力被"字符串拼接"实现，丧失工程化价值 | 高 | 必须以 `AiCapability` 接口 + Adapter 模式实现 |
| R4 | 数据库选型导致本地无法演示 | 中 | 默认 MySQL 8 |
| R5 | 前端范围扩张导致时间失控 | 中 | 前端最小化（单页 + 列表 + 详情 + 弹窗） |
| R6 | 测试覆盖不足 | 中 | 至少 4 类核心单测，CI 不强制但本地必须通过 |
| R7 | LLM Mock 输出不稳定 | 中 | 适配层用强 schema（Jackson + DTO）约束输出 |
| R8 | 时间紧张导致加分项挤压必做项 | 高 | 严格按 §8 必做验收项先闭环，加分项最后做 |
| R9 | 与团队规范不一致 | 中 | 编码前过一遍 `group_development_rule.md` |
| R10 | 错误码/返回体不统一 | 中 | 提前定义 `Result` / `BizException` / 错误码字典 |

---

## 10. 进入下一步的前置条件

在进入"任务拆解 / 方案设计 / API 设计"前，需完成：

1. §7 待确认问题至少对齐 Q1、Q2、Q3、Q4、Q5、Q6、Q10、Q11、Q12。
2. §9 风险 R2 给出明确业务规则定义。
3. 状态机白名单流转图定稿。
4. AI 能力范围定稿（至少 1 种，最多 3 种）。
5. 数据库与 Mock 策略定稿。

> 本文档不进入编码阶段，仅作为后续方案设计、API 设计、任务拆解、代码生成的输入。
