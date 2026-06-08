# API 设计说明

> 本文档基于 `docs/templates/api-design-proposal.md` 模板撰写，对照实际代码（`backend/src/main/java/com/sdpm/workitem/**`）梳理当前已实现的 API 契约。
> 适用项目：AI 辅助研发工作项流转与需求澄清系统（SDPM WorkItem）。
> 关联文档：
> - 提交规范：[./02_提交规范.md](./02_提交规范.md)
> - 系统设计：[./design/api-design.md](./design/api-design.md)
> - 架构设计：[./design/architecture.md](./design/architecture.md)
> - 需求理解：[./requirements/requirement.md](./requirements/requirement.md)
> - 数据库脚本：[./database/db.sql](./database/db.sql)
> - 编码规范：[../.trae/rules/group_development_rule.md](../.trae/rules/group_development_rule.md)

---

## 1. API 设计目标

| 编号 | 目标 | 体现位置 |
|---|---|---|
| G1 | 支撑工作项全生命周期（创建 / 详情 / 修改 / 删除 / 状态流转） | §3.1 |
| G2 | 状态流转规则化、可审计、可阻断（P0 澄清问题） | §3.2、§4 |
| G3 | 澄清问题沉淀为可挂载、可解决的独立资源 | §3.3 |
| G4 | AI 能力以可插拔方式暴露给前端，支持 MOCK / LLM 双源 | §3.4、§5 |
| G5 | 字典驱动枚举展示，降低前后端耦合 | §3.5 |
| G6 | 统一返回体 `Result<T>` + 统一错误码，全栈只需判断 `code` | §3 通用约定（见设计文档）、§4 |
| G7 | Controller 仅做参数接收 / 校验 / 调度，业务在 Service | 编码规范 §3.1 |

---

## 2. 资源或模块划分

### 2.1 资源关系

```text
┌──────────────┐ 1   N ┌────────────────────────┐
│ work_item    │──────►│ work_item_status_history│
│ (聚合根)      │       └────────────────────────┘
│              │ 1   N ┌────────────────────────┐
│              │──────►│ clarification_question   │
│              │       └────────────────────────┘
│              │ 1   N ┌────────────────────────┐
│              │──────►│ ai_analysis_result       │
└──────────────┘       └────────────────────────┘

┌──────────────┐
│ dict_item    │   独立字典表
└──────────────┘
```

### 2.2 模块清单

| 模块 | 路径前缀 | Controller | 关键 Service |
|---|---|---|---|
| 工作项（workitem） | `/api/v1/work-items` | `WorkItemController` | `WorkItemService` |
| 状态流转（transition） | `/api/v1/work-items/{id}/transitions` | `WorkItemTransitionController` | `WorkItemTransitionService` + `StateMachine` + `P0ClarificationGuard` |
| 澄清问题（clarification） | `/api/v1/work-items/{id}/clarifications` + `/api/v1/clarifications/{cid}` | `ClarificationController` | `ClarificationService` |
| AI 分析（ai） | `/api/v1/work-items/{id}/ai-analyses` | `AiAnalysisController` | `AiAnalysisService` + `AiAdapter`（`MockAiAdapter`） |
| 字典（dict） | `/api/v1/dicts` | `DictController` | `DictService` |

### 2.3 模块职责

- **workitem**：CRUD + 详情 + 分页；不暴露 status 修改（status 走 transition）。
- **transition**：封装状态机与守卫链，状态变更必落 `work_item_status_history`。
- **clarification**：独立子资源，OPEN / RESOLVED 二态，触发 P0 阻断。
- **ai**：以 `analysisType` 路由到 `AiCapability` 实现，返回符合 schema 的 `payload`。
- **dict**：通用字典，由前端按 `type` 拉取缓存，避免后端硬编码枚举展示。

---

## 3. API 列表

> 标注：
> - **Auth**：Y = 需 `X-User` 头（无值时记 `anonymous`，写接口不阻断）；N = 无要求
> - **Idempotent**：当前实现未在 Controller 层强制 `Idempotency-Key`，由业务层（乐观锁、状态机）保证幂等
> - **Code**：成功时 `Result.code=0`；失败时取 §4 错误码

### 3.1 工作项（workitem）

| 能力 | 方法 / 路径 | 输入摘要 | 输出摘要 | Auth | 说明 |
|---|---|---|---|---|---|
| 创建工作项 | `POST /api/v1/work-items` | `WorkItemCreateReqDTO`（`title`/`type` 必填；`priority` 缺省 `P2`；`code`/`description`/`assignee`/`reporter`/`tags`/`acceptanceCriteria` 可选） | `WorkItemRespVO`（初始 `status=DRAFT`） | Y | `code` 重复 → `BIZ_DUPLICATE_CODE` |
| 修改工作项 | `PUT /api/v1/work-items/{id}` | `WorkItemUpdateReqDTO`（`version` 必填；`type`/`code`/`status`/`reporter` 不可改） | `WorkItemRespVO` | Y | `@Version` 乐观锁；不匹配 → `BIZ_VERSION_CONFLICT`；DONE 不可改 → `BIZ_DONE_IMMUTABLE` |
| 工作项详情 | `GET /api/v1/work-items/{id}` | — | `WorkItemDetailRespVO`（含 `p0OpenClarifications` / `totalOpenClarifications` / `lastTransitionTime`） | N | 不存在 → `BIZ_NOT_FOUND` |
| 工作项分页 | `GET /api/v1/work-items` | `WorkItemQueryReqDTO`（`pageNo`/`pageSize`/`keyword`/`type`/`priority`/`status`/`assignee`/`reporter`/`sortBy`/`sortDir`） | `PageResp<WorkItemRespVO>` | N | `sortBy+sortDir` 显式排序；缺省 `createdAt DESC` |
| 逻辑删除 | `DELETE /api/v1/work-items/{id}` | — | `Result<Boolean>`（`data=true`） | Y | DONE 不可删 → `BIZ_DONE_IMMUTABLE` |

#### 3.1.1 创建请求体 `WorkItemCreateReqDTO`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `title` | String | 是 | `@NotBlank @Size(max=255)` | 标题 |
| `code` | String | 否 | `@Size(max=64)` | 业务编码；缺省服务端生成 |
| `description` | String | 否 | `@Size(max=10000)` | 描述 |
| `type` | String | 是 | `@NotBlank` | `STORY` / `BUG` / `TASK` |
| `priority` | String | 否 | 缺省 `P2` | `P0` / `P1` / `P2` / `P3` |
| `assignee` | String | 否 | `@Size(max=64)` | 负责人 |
| `reporter` | String | 否 | `@Size(max=64)` | 提出人；缺省取 `X-User` |
| `tags` | List\<String\> | 否 | — | 标签 |
| `acceptanceCriteria` | List\<String\> | 否 | — | 验收标准 |

#### 3.1.2 修改请求体 `WorkItemUpdateReqDTO`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `title` | String | 否 | `@Size(max=255)` | 标题 |
| `description` | String | 否 | `@Size(max=10000)` | 描述 |
| `priority` | String | 否 | — | 优先级 |
| `assignee` | String | 否 | `@Size(max=64)` | 负责人 |
| `tags` | List\<String\> | 否 | — | 标签 |
| `acceptanceCriteria` | List\<String\> | 否 | — | 验收标准 |
| `version` | Long | 是 | `@NotNull @Min(0)` | 乐观锁；不匹配返回 `BIZ_VERSION_CONFLICT` |

#### 3.1.3 查询请求体 `WorkItemQueryReqDTO`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `pageNo` | Integer | 否 | `@Min(1)`；缺省 1 | 当前页 |
| `pageSize` | Integer | 否 | `@Min(1) @Max(100)`；缺省 10 | 每页条数 |
| `keyword` | String | 否 | `@Size(max=64)` | 模糊匹配 `title` / `code` |
| `type` | String | 否 | — | 类型筛选 |
| `priority` | String | 否 | — | 优先级筛选 |
| `status` | String | 否 | — | 状态筛选 |
| `assignee` | String | 否 | — | 负责人筛选 |
| `reporter` | String | 否 | — | 提出人筛选 |
| `sortBy` | String | 否 | 缺省 `createdAt` | 排序字段 |
| `sortDir` | String | 否 | 缺省 `desc` | 排序方向 |

#### 3.1.4 响应体 `WorkItemRespVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键 |
| `code` | String | 业务编码 |
| `title` | String | 标题 |
| `description` | String \| null | 描述 |
| `type` | String | 类型 |
| `priority` | String | 优先级 |
| `status` | String | 状态（创建时为 `DRAFT`） |
| `riskLevel` | String \| null | 风险等级 |
| `assignee` | String \| null | 负责人 |
| `reporter` | String \| null | 提出人 |
| `tags` | List\<String\> | 标签 |
| `acceptanceCriteria` | List\<String\> | 验收标准 |
| `version` | Long | 乐观锁版本 |
| `createdAt` | LocalDateTime | 创建时间（Jackson 默认 ISO-8601） |
| `updatedAt` | LocalDateTime | 更新时间 |

#### 3.1.5 详情响应体 `WorkItemDetailRespVO`

在 `WorkItemRespVO` 基础上新增：

| 字段 | 类型 | 说明 |
|---|---|---|
| `p0OpenClarifications` | int | 未解决 P0 澄清数（前端据此禁用流转按钮） |
| `totalOpenClarifications` | int | 未解决澄清总数 |
| `lastTransitionTime` | LocalDateTime \| null | 最近一次流转时间 |

---

### 3.2 状态流转（transition）

| 能力 | 方法 / 路径 | 输入摘要 | 输出摘要 | Auth | 说明 |
|---|---|---|---|---|---|
| 触发流转 | `POST /api/v1/work-items/{id}/transitions` | `WorkItemTransitionReqDTO`（`targetStatus` 必填；`reason` 可选 ≤500） | `WorkItemTransitionRespVO` | Y | 见 §4 守卫链 |
| 状态历史 | `GET /api/v1/work-items/{id}/transitions` | Query：`pageNo`/`pageSize`（缺省 1/20） | `PageResp<WorkItemStatusHistoryRespVO>` | N | — |

#### 3.2.1 流转请求体 `WorkItemTransitionReqDTO`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `targetStatus` | String | 是 | `@NotBlank` | 目标状态；必须出现在 `WorkItemStatusEnum` |
| `reason` | String | 否 | `@Size(max=500)` | 变更原因 / 备注 |

#### 3.2.2 流转响应体 `WorkItemTransitionRespVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `workItemId` | Long | 工作项 ID |
| `fromStatus` | String \| null | 变更前状态 |
| `toStatus` | String | 变更后状态 |
| `operator` | String | 操作人（来自 `X-User`，缺省 `anonymous`） |
| `transitionedAt` | String | 流转时间 |
| `historyId` | Long | 状态历史记录 ID |

#### 3.2.3 状态历史响应体 `WorkItemStatusHistoryRespVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 历史 ID |
| `workItemId` | Long | 工作项 ID |
| `fromStatus` | String | 变更前状态 |
| `toStatus` | String | 变更后状态 |
| `reason` | String \| null | 原因 |
| `operator` | String | 操作人 |
| `createdAt` | String | 变更时间 |

---

### 3.3 澄清问题（clarification）

| 能力 | 方法 / 路径 | 输入摘要 | 输出摘要 | Auth | 说明 |
|---|---|---|---|---|---|
| 新增澄清 | `POST /api/v1/work-items/{id}/clarifications` | `ClarificationCreateReqDTO`（`question` 必填 ≤2000；`severity` 可选 `P0/P1/P2` 缺省 `P1`；`raisedBy` 可选） | `ClarificationRespVO` | Y | 同 `(workItemId, question)` → `BIZ_DUPLICATE_QUESTION` |
| 解决澄清 | `PUT /api/v1/clarifications/{cid}` | `ClarificationResolveReqDTO`（`answer` 必填 ≤2000；`resolvedBy` 可选） | `ClarificationRespVO` | Y | 二次解决 → `BIZ_CLARIFICATION_ALREADY_RESOLVED`；**不**修改工作项 status |
| 澄清列表 | `GET /api/v1/work-items/{id}/clarifications` | Query：`severity`/`status`/`pageNo`/`pageSize`（缺省 1/20） | `PageResp<ClarificationRespVO>` | N | — |

#### 3.3.1 响应体 `ClarificationRespVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键 |
| `workItemId` | Long | 工作项 ID |
| `question` | String | 问题 |
| `severity` | String | 严重程度 |
| `status` | String | `OPEN` / `RESOLVED` |
| `answer` | String \| null | 回答（新增时为 null） |
| `raisedBy` | String \| null | 提出人 |
| `resolvedBy` | String \| null | 解决人 |
| `createdAt` | String | 提出时间 |
| `resolvedAt` | String \| null | 解决时间 |

---

### 3.4 AI 分析（ai）

| 能力 | 方法 / 路径 | 输入摘要 | 输出摘要 | Auth | 说明 |
|---|---|---|---|---|---|
| 触发分析 | `POST /api/v1/work-items/{id}/ai-analyses` | `AiAnalysisTriggerReqDTO`（`analysisType` 必填；`forceRefresh` 缺省 false） | `AiAnalysisRespVO` | Y | 详见 §5 |
| AI 历史 | `GET /api/v1/work-items/{id}/ai-analyses` | Query：`analysisType`/`pageNo`/`pageSize`（缺省 1/10） | `PageResp<AiAnalysisRespVO>` | N | — |

#### 3.4.1 触发请求体 `AiAnalysisTriggerReqDTO`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `analysisType` | String | 是 | `@NotBlank` | `SUMMARY` / `ACCEPTANCE` / `RISK` / `CLARIFICATION` / `TASK_BREAKDOWN` |
| `forceRefresh` | Boolean | 否 | 缺省 false | 是否强制重新分析 |

#### 3.4.2 响应体 `AiAnalysisRespVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 分析结果 ID |
| `workItemId` | Long | 工作项 ID |
| `analysisType` | String | 分析类型 |
| `source` | String | `MOCK` / `LLM` |
| `payload` | Map\<String, Object\> | 结构化结果（强 schema 校验，详见 §5） |
| `summary` | String | 一句话摘要 |
| `createdAt` | String | 生成时间 |

---

### 3.5 字典（dict）

| 能力 | 方法 / 路径 | 输入摘要 | 输出摘要 | Auth | 说明 |
|---|---|---|---|---|---|
| 查询字典 | `GET /api/v1/dicts?type=...&enabledOnly=...` | `type` 必填；`enabledOnly` 缺省 true | `Result<List<DictRespVO>>` | N | — |
| 创建字典项 | `POST /api/v1/dicts` | `DictCreateReqDTO`（`type`/`key`/`label` 必填；`value`/`sort`/`enabled` 可选） | `DictRespVO` | Y（当前未做角色校验） | `(type,key)` 重复 → `BIZ_DUPLICATE_DICT_KEY` |
| 修改字典项 | `PUT /api/v1/dicts/{id}` | `DictUpdateReqDTO`（`label`/`value`/`sort`/`enabled` 任意子集） | `DictRespVO` | Y | — |
| 删除字典项 | `DELETE /api/v1/dicts/{id}` | — | `Result<Boolean>` | Y | 引用中 → `BIZ_DICT_IN_USE`；不存在视为成功 |

#### 3.5.1 响应体 `DictRespVO`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键 |
| `type` | String | 字典类型 |
| `key` | String | 字典项 key |
| `label` | String | 展示名 |
| `value` | String \| null | 业务值 |
| `sort` | Integer | 排序 |
| `enabled` | Boolean | 是否启用 |

---

### 3.6 通用约定

- **统一返回体**：`Result<T> = { code:int, message:string, data:T|null, timestamp:long }`。
- **分页返回体**：`PageResp<T> = { pageNo:int, pageSize:int, total:long, records:List<T> }`。
- **请求头**：
  - `Content-Type: application/json; charset=UTF-8`
  - `X-User: <operator>`（写接口建议带；缺省 `anonymous`，由 `UserContextFilter` 读取写入 `UserContext`）
- **HTTP 状态码**：`GlobalExceptionHandler` 对 `BizException` 一律返回 `200 OK`，错误通过 `code` 字段表达；参数校验失败返回 `400`；未捕获异常返回 `500`。
- **时间格式**：
  - 入参：JSON 字符串 / ISO-8601（由 Jackson 解析）；
  - 出参：`WorkItemRespVO` / `WorkItemDetailRespVO` 字段为 `LocalDateTime`（Jackson 默认 ISO-8601，例如 `2026-06-08T10:00:00`）；其余 VO 字段为 `String`，由 Service 格式化。
- **字段命名**：`lowerCamelCase`；枚举值字符串（`DRAFT`、`P0`、`OPEN`）。

---

## 4. 状态流转错误设计

### 4.1 状态机邻接表

`StateMachine` 中实际定义的 6 个状态及合法迁移：

```text
DRAFT       → {ANALYZING}
ANALYZING   → {READY, DRAFT}
READY       → {IN_PROGRESS, ANALYZING}
IN_PROGRESS → {IN_TESTING, READY}
IN_TESTING  → {DONE, IN_PROGRESS}
DONE        → {}                  // 终态，不可流转
```

> 状态机以 `Map<WorkItemStatusEnum, Set<WorkItemStatusEnum>>` 形式硬编码于 `StateMachine.TRANSITIONS`，扩展时需同步更新枚举与单元测试。

### 4.2 守卫链

`WorkItemTransitionServiceImpl` 顺序执行：

1. **`StateMachine.assertTransit(from, to)`**：
   - 源/目标为 `DONE` → `BIZ_DONE_IMMUTABLE`（409）
   - 不在邻接表 → `BIZ_TRANSITION_NOT_ALLOWED`（409，message 含起止状态）
2. **`P0ClarificationGuard.check(workItem, target)`**：
   - 仅当 `target ∈ {READY, IN_PROGRESS, IN_TESTING, DONE}` 触发；
   - 统计 `(workItemId, severity=P0, status=OPEN)` 数量，`> 0` → `BIZ_P0_CLARIFICATION_BLOCKED`（409，message 含条数与目标状态）。

### 4.3 错误码对照

| 错误码 | 含义 | 触发场景 | HTTP（按实现） |
|---|---|---|---|
| `BIZ_TRANSITION_NOT_ALLOWED` | 状态流转不合法 | 邻接表外的跳级流转 | 200（body.code=409） |
| `BIZ_DONE_IMMUTABLE` | DONE 不可变更 | DONE 源/目标 / DONE 上修改工作项 / 逻辑删除 DONE | 200（body.code=409） |
| `BIZ_P0_CLARIFICATION_BLOCKED` | P0 未解决阻断流转 | 进入 `READY/IN_PROGRESS/IN_TESTING/DONE` 仍有 P0 OPEN | 200（body.code=409） |
| `BIZ_VERSION_CONFLICT` | 乐观锁冲突 | 修改工作项时 `version` 不匹配 | 200（body.code=409） |
| `BIZ_CLARIFICATION_ALREADY_RESOLVED` | 澄清已解决 | 二次解决 | 200（body.code=409） |
| `BIZ_NOT_FOUND` | 资源不存在 | 工作项 / 澄清问题 / 字典项 / AI 结果不存在 | 200（body.code=404） |
| `BIZ_DUPLICATE_CODE` | 业务编码重复 | 创建工作项时 `code` 已存在 | 200（body.code=409） |
| `BIZ_DUPLICATE_QUESTION` | 同一工作项下重复问题 | `(work_item_id, question)` 已存在 | 200（body.code=409） |
| `BIZ_DUPLICATE_DICT_KEY` | 字典 `(type, key)` 重复 | 字典创建时违反唯一约束 | 200（body.code=409） |
| `BIZ_DICT_IN_USE` | 字典被引用 | 存在引用时删除字典项 | 200（body.code=409） |
| `BIZ_PARAM_INVALID` | 参数校验失败 | `@Valid` 失败 / JSON 不可读 | 400 |
| `BIZ_AI_CAPABILITY_NOT_FOUND` | 不支持的 AI 类型 | `analysisType` 不在枚举内 | 200（body.code=400） |
| `BIZ_AI_SCHEMA_INVALID` | AI 结果 schema 不符 | payload 缺失关键 key / 类型不匹配 | 200（body.code=500） |
| `BIZ_AI_UPSTREAM_FAILURE` | 上游 AI 不可用 | LLM 模式调用失败 / 超时 | 200（body.code=502） |
| `BIZ_FORBIDDEN` | 权限不足 | 预留，当前实现未触发 | 200（body.code=403） |
| `SYS_INTERNAL` | 系统异常 | 未捕获的 `Exception` | 500 |

> 注：当前 `GlobalExceptionHandler` 对 `BizException` 统一返回 `200 OK` + `Result.error(code, message)`；前端必须以 `Result.code` 而非 HTTP 状态码判断业务结果。

### 4.4 错误响应示例

```json
{
  "code": 409,
  "message": "存在 1 条未解决的P0澄清问题，无法进入 待开发",
  "data": null,
  "timestamp": 1717828900000
}
```

---

## 5. AI 分析结果设计

### 5.1 路由与适配

- `AiAnalysisService.triggerAnalysis(workItemId, type)`：
  1. 校验 `type` 在 `AiAnalysisTypeEnum` 内，否则 `BIZ_AI_CAPABILITY_NOT_FOUND`；
  2. 加载工作项，缺失则 `BIZ_NOT_FOUND`；
  3. 通过 `AiAdapter`（当前实现为 `MockAiAdapter`）调用对应 `AiCapability`；
  4. 对返回的 `Map<String,Object> payload` 做 schema 校验，失败 → `BIZ_AI_SCHEMA_INVALID`；
  5. 写入 `ai_analysis_result` 表（不更新工作项主表）。
- `forceRefresh=true` 跳过历史复用，每次都触发新的分析；当前实现未在 `triggerAnalysis` 内部使用该字段（保留为未来扩展点）。

### 5.2 强 schema `payload`

每种 `analysisType` 在代码侧注册必填 key（由 `AiCapability` 实现与 Mock 模板共同约束）：

| analysisType | 必填 key | 备注 |
|---|---|---|
| `SUMMARY` | `headline` / `background` / `goal` / `scope` / `risks` / `keyPoints` | 字符串 + 字符串数组 |
| `ACCEPTANCE` | `criteria`（`given`/`when`/`then`）/ `coverage` | Gherkin 风格 |
| `RISK` | `level`（`LOW`/`MEDIUM`/`HIGH`）/ `items`（`type`/`desc`/`mitigation`） | — |
| `CLARIFICATION` | `questions`（`question`/`severity`/`reason`） | severity ∈ `P0/P1/P2` |
| `TASK_BREAKDOWN` | `tasks`（`title`/`estimateHours`/`ownerHint`）/ `totalEstimateHours` | ownerHint ∈ `frontend/backend/qa` |

> 校验失败时 `BIZ_AI_SCHEMA_INVALID` 由 `GlobalExceptionHandler` 转为 `Result.error`，**不会**写入数据库，便于排查。

### 5.3 出参 `AiAnalysisRespVO`

```json
{
  "id": 9001,
  "workItemId": 1001,
  "analysisType": "SUMMARY",
  "source": "MOCK",
  "summary": "本工作项关注登录失败时的前端兜底体验。",
  "payload": {
    "headline": "登录失败前端兜底",
    "background": "登录 5xx 时缺乏用户反馈",
    "goal": "2s 内反馈 + 幂等重试",
    "scope": ["前端 toast", "重试按钮", "幂等键"],
    "risks": ["重试引发重复登录"],
    "keyPoints": ["统一 request 拦截器捕获 5xx"]
  },
  "createdAt": "2026-06-08 10:10:00"
}
```

### 5.4 后续替换为 LLM

- 新增 `LlmAiAdapter implements AiAdapter`，由 Spring 注入切换；
- 调整 `payload` 校验为 prompt 后置 schema 校验；
- 新增 `BIZ_AI_UPSTREAM_FAILURE` 错误码（已就位）。

---

## 6. 前后端协作说明

### 6.1 前端调用方式

- 前端仓库：`frontend/src/api/*.js`（`workitem.js` / `transition.js` / `clarification.js` / `ai.js` / `dict.js`）。
- 统一请求封装：`frontend/src/utils/request.js`：
  - 拦截器在响应后判断 `Result.code`：`=== 0` 放行 resolve，否则 reject 并 toast `Result.message`；
  - 列表 / 详情 / 弹窗三处复用：`WorkItemList` / `WorkItemDetail` / `TransitionDialog` / `ClarificationDialog` / `AiAnalysisDialog`。
- `X-User` 由前端从 `useUserStore` 注入，缺省 `anonymous`。

### 6.2 Mock → 真实后端切换

- 前端在 `frontend/src/utils/request.js` 中通过 `baseURL` 与 `VITE_API_BASE_URL` 切换；
- 当前前端默认指向 `http://localhost:8080/api/v1`，与后端 `application.yml` 的 `server.port=8080` 对齐；
- 字典数据：前端按 `type` 缓存 5 分钟，缺省从枚举内置 `desc` 兜底；
- 错误码翻译：前端维护一份 `errorCodeMap.json`，按 `code` 展示精细化提示（如 `BIZ_P0_CLARIFICATION_BLOCKED` 引导「先去解决 P0」）。

### 6.3 状态机协作约定

- 前端在工作项详情页读取 `p0OpenClarifications`：
  - `> 0` 时禁用进入 `READY/IN_PROGRESS/IN_TESTING/DONE` 的按钮；
  - 流转按钮的可用目标集合可由前端预计算（与后端 `StateMachine.TRANSITIONS` 一致），后端为最终事实源。
- 状态历史与流转响应**总是**伴随一条 `work_item_status_history` 写入（同一事务）。

### 6.4 错误处理约定

- 后端：`BizException` → `Result.error(code, message)`；`MethodArgumentNotValidException` → `400 + BIZ_PARAM_INVALID`；未捕获异常 → `500 + SYS_INTERNAL`。
- 前端：拦截器在 `code !== 0` 时统一 toast `message`，按需提示重试或跳详情；不直接展示技术堆栈。

### 6.5 字典使用约定

| 字典 type | 用途 |
|---|---|
| `WORK_ITEM_STATUS` | 状态展示与筛选 |
| `WORK_ITEM_TYPE` | 类型展示与筛选 |
| `WORK_ITEM_PRIORITY` | 优先级展示与筛选 |
| `CLARIFICATION_SEVERITY` | 澄清严重度 |
| `RISK_LEVEL` | 风险等级 |

---

## 7. 后续扩展

| 优先级 | 能力 | 影响面 |
|---|---|---|
| P1 | 引入 JWT 鉴权，替换 `X-User` 直传 | 新增 `AuthFilter`；`UserContext` 扩展 `userId`/`role`/`tenantId`；`X-Role: admin` 真正生效 |
| P1 | 引入 `Idempotency-Key` 写接口幂等（当前依赖乐观锁 / 业务约束） | 新增 `IdempotencyStore`；写接口补 `Idempotency-Key` 必填 |
| P1 | `OpenAPI 3` 文档（springdoc-openapi）自动生成并部署 | 启动 `/v3/api-docs` + `/swagger-ui.html` |
| P2 | 工作项搜索（ES / 简单全文） | 新增 `/api/v1/work-items/search` |
| P2 | 状态流转异步通知（WebSocket / SSE） | 新增 `/api/v1/work-items/{id}/transitions/stream` |
| P2 | AI 异步任务 + 轮询 | `trigger` 返回 `taskId`，新增 `/ai-analyses/{taskId}`；`LlmAiAdapter` 接入 |
| P2 | 归档状态 `ARCHIVED` | `WorkItemStatusEnum` 增项；状态机邻接表补 `→ ARCHIVED`；DTO 校验同步 |
| P3 | 批量操作（批量流转、批量解决澄清） | 新增 `POST /api/v1/work-items/batch/...` |
| P3 | 审计日志接口 | 新增 `/api/v1/audit-logs` 模块 |
| P3 | 多租户隔离 | 全部列表接口增加 `tenantId` 过滤；DB 增加 `tenant_id` 列 |

---

> 本文档与 `docs/design/api-design.md` 在以下方面存在差异，请以本文档（`api-design-proposal.md`）与代码实现为准：
> 1. 状态枚举实际为 6 个（`DRAFT`/`ANALYZING`/`READY`/`IN_PROGRESS`/`IN_TESTING`/`DONE`），无 `ARCHIVED`；
> 2. 错误码在响应体 `code` 字段中，HTTP 状态码统一为 `200 OK`（除参数校验 400 / 系统异常 500）；
> 3. 当前实现未在 Controller 层强制 `Idempotency-Key`，幂等由乐观锁、状态机、业务约束保证；
> 4. 当前 `DictController` 未校验 `X-Role: admin`，`BIZ_FORBIDDEN` 为预留错误码。
