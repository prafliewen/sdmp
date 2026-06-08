# API 契约设计：AI 辅助研发工作项流转与需求澄清系统

> 文档目的：基于 `docs/design/architecture.md` 输出**对外可消费**的 API 契约，作为前后端联调、接口评审、契约测试、第三方接入的统一基线。
> 适用范围：`/api/v1` 前缀下所有 REST 接口。
> 关联文档：
> - 系统设计：[architecture.md](./architecture.md)
> - 需求理解：[../requirements/requirement.md](../requirements/requirement.md)
> - 任务拆解：[../tasks/breakdown.md](../tasks/breakdown.md)
> - 数据库脚本：[../database/db.sql](../database/db.sql)
> - 编码规范：[../../.trae/rules/group_development_rule.md](../../.trae/rules/group_development_rule.md)

---

## 1. 设计目标

| 编号 | 目标 | 体现位置 |
|---|---|---|
| A1 | 接口语义清晰、REST 风格统一，前端可零成本理解 | §3 接口列表 |
| A2 | 统一返回体 `Result<T>` + 统一错误码，前端只判 `code` | §4 通用约定、§6 错误码 |
| A3 | 鉴权最小化（`X-User` 头），未来可平滑升级 JWT | §5 鉴权 |
| A4 | 关键写接口支持幂等，避免重复创建 / 重复触发 | §7 幂等 |
| A5 | 与架构文档严格一致（路径、状态机、守卫、AI 能力均不偏离） | 全文 |
| A6 | 每个接口含请求/响应示例，便于联调 | §8 示例 |

---

## 2. 资源与模块划分

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

### 2.2 路径前缀与版本

- 统一前缀：`/api/v1`
- 文档入口：`/v3/api-docs`（springdoc-openapi，启动后自动暴露）
- 健康检查：`/actuator/health`

### 2.3 模块清单

| 模块 | 路径前缀 | Controller |
|---|---|---|
| 工作项 | `/api/v1/work-items` | `WorkItemController` |
| 状态流转 | `/api/v1/work-items/{id}/transitions` | `WorkItemTransitionController` |
| 澄清问题 | `/api/v1/work-items/{id}/clarifications` + `/api/v1/clarifications` | `ClarificationController` |
| AI 分析 | `/api/v1/work-items/{id}/ai-analyses` | `AiAnalysisController` |
| 字典 | `/api/v1/dicts` | `DictController` |
| 健康 | `/actuator/health` | Spring Boot Actuator |

---

## 3. 通用约定

### 3.1 统一返回体

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | Integer | 0 = 成功；非 0 见 §6 错误码字典 |
| `message` | String | 人类可读提示 |
| `data` | Object / Array / Null | 业务数据；无数据时为 `null` |
| `timestamp` | Long (ms) | 服务端生成时间戳 |

### 3.2 分页返回体

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "pageNo": 1,
    "pageSize": 10,
    "total": 100,
    "records": []
  },
  "timestamp": 1710000000000
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `pageNo` | Integer | 当前页（从 1 开始） |
| `pageSize` | Integer | 每页条数（1-100） |
| `total` | Long | 总记录数 |
| `records` | Array | 数据列表 |

### 3.3 请求头约定

| Header | 必填 | 用途 |
|---|---|---|
| `Content-Type` | 是 | `application/json; charset=UTF-8` |
| `X-User` | 否 | 当前操作人；缺省 `anonymous` |
| `X-Role` | 否 | `admin` / `user`；字典维护等管理接口需 `admin` |
| `X-Request-Id` | 否 | 链路追踪；缺省服务端生成 UUID |
| `Idempotency-Key` | 视接口 | 幂等键，见 §7 |

### 3.4 时间格式

- 入参：`yyyy-MM-dd HH:mm:ss` 或 ISO-8601 `yyyy-MM-ddTHH:mm:ssZ`
- 出参：默认 `yyyy-MM-dd HH:mm:ss`（VO 字段类型 `String`）
- 时间戳：`timestamp` 字段为毫秒（Long）

### 3.5 字段命名

- 入参 / 出参：lowerCamelCase（`pageNo`、`workItemId`）。
- 枚举值：大写下划线字符串（`WORK_ITEM_STATUS`、`P0`）。
- 不返回 Entity 字段命名（`created_at` → `createdAt`），由 VO 转换。

---

## 4. 鉴权

### 4.1 当前形态

| 维度 | 方案 |
|---|---|
| 登录态 | 无；通过 `X-User` 头传递 |
| 缺省值 | 未携带时为 `anonymous` |
| 角色 | `X-Role: admin` 识别管理员；普通接口不校验角色 |
| 写接口 | 所有写接口必带 `X-User`（缺省不阻断但会记录 `anonymous`） |
| 读接口 | 不强制 |

### 4.2 升级路径

- 后续可在 `UserContextFilter` 之前插入 `AuthFilter`：
  1. 解析 `Authorization: Bearer <jwt>`；
  2. 写入 `UserContext.userId / role / tenantId`；
  3. 删除 / 弱化 `X-User` 直传。
- 接口层与业务层不感知差异，仅依赖 `UserContext`。

### 4.3 写接口的最小鉴权（当前）

| 接口 | 所需角色 |
|---|---|
| 创建 / 修改 / 流转 / 解决澄清 / 触发 AI | 任意已标识用户（`X-User` 非空） |
| 字典维护（POST/PUT/DELETE `/dicts`） | `X-Role: admin` |
| 读接口 | 无 |

---

## 5. 接口列表

> 标注说明：
> - **Auth**：Y = 需 `X-User`；A = 需 `X-Role: admin`；N = 无要求
> - **Idempotent**：Y = 支持幂等键（见 §7）；N = 天然幂等或仅查询

### 5.1 工作项（workitem）

#### 5.1.1 创建工作项

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `POST /api/v1/work-items` |
| Auth | Y |
| Idempotent | Y（`Idempotency-Key` 必填） |

请求体 `WorkItemCreateReqDTO`：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `code` | String | 否 | `@Size(max=64)` | 业务编码；缺省服务端生成 `WI-YYYYMMDD-序号` |
| `title` | String | 是 | `@NotBlank @Size(max=255)` | 标题 |
| `description` | String | 否 | `@Size(max=10000)` | 详细描述 |
| `type` | String | 是 | `@NotBlank @Pattern(STORY\|BUG\|TASK)` | 类型 |
| `priority` | String | 否 | `@Pattern(P0\|P1\|P2\|P3)` | 优先级；缺省 `P2` |
| `assignee` | String | 否 | `@Size(max=64)` | 负责人 |
| `reporter` | String | 否 | `@Size(max=64)` | 提出人；缺省取 `X-User` |
| `tags` | List\<String\> | 否 | `@Size(max=20)` | 标签列表 |
| `acceptanceCriteria` | List\<String\> | 否 | `@Size(max=20)` | 验收标准 |

响应 `Result<WorkItemRespVO>`，data 结构：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键 |
| `code` | String | 业务编码 |
| `title` | String | 标题 |
| `description` | String\|null | 描述 |
| `type` | String | 类型 |
| `priority` | String | 优先级 |
| `status` | String | 始终为 `DRAFT` |
| `riskLevel` | String\|null | 风险等级（创建时为 null） |
| `assignee` | String\|null | 负责人 |
| `reporter` | String\|null | 提出人 |
| `tags` | List\<String\> | 标签 |
| `acceptanceCriteria` | List\<String\> | 验收标准 |
| `version` | Long | 乐观锁 |
| `createdAt` | String | 创建时间 |
| `updatedAt` | String | 更新时间 |

错误码：`BIZ_PARAM_INVALID` / `BIZ_DUPLICATE_CODE` / `SYS_INTERNAL`

---

#### 5.1.2 修改工作项（不含 status）

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `PUT /api/v1/work-items/{id}` |
| Auth | Y |
| Idempotent | N（依赖 `@Version` 乐观锁） |

路径参数：`id`（Long，工作项主键）

请求体 `WorkItemUpdateReqDTO`：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `title` | String | 否 | `@Size(max=255)` | 标题 |
| `description` | String | 否 | `@Size(max=10000)` | 描述 |
| `priority` | String | 否 | `@Pattern(P0\|P1\|P2\|P3)` | 优先级 |
| `assignee` | String | 否 | `@Size(max=64)` | 负责人 |
| `tags` | List\<String\> | 否 | `@Size(max=20)` | 标签 |
| `acceptanceCriteria` | List\<String\> | 否 | `@Size(max=20)` | 验收标准 |
| `version` | Long | 是 | `@NotNull @Min(0)` | 乐观锁；不匹配返回 409 |

> 注：`type` / `code` / `status` / `reporter` 不允许通过此接口修改。

响应：`Result<WorkItemRespVO>`（同 5.1.1）

错误码：`BIZ_PARAM_INVALID` / `BIZ_NOT_FOUND` / `BIZ_VERSION_CONFLICT` / `BIZ_DONE_IMMUTABLE`

---

#### 5.1.3 工作项详情

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `GET /api/v1/work-items/{id}` |
| Auth | N |
| Idempotent | — |

响应：`Result<WorkItemDetailRespVO>`，data 在 5.1.1 基础上增加：

| 字段 | 类型 | 说明 |
|---|---|---|
| `p0OpenCount` | Integer | 未解决 P0 澄清问题数（供前端禁用流转按钮） |
| `clarificationOpenCount` | Integer | 总未解决澄清数 |
| `lastTransitionAt` | String\|null | 最近一次流转时间 |

错误码：`BIZ_NOT_FOUND`

---

#### 5.1.4 工作项分页查询

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `GET /api/v1/work-items` |
| Auth | N |
| Idempotent | — |

Query 参数 `WorkItemQueryReqDTO`：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `pageNo` | Integer | 否 | `@Min(1)`；缺省 1 | 当前页 |
| `pageSize` | Integer | 否 | `@Min(1) @Max(100)`；缺省 10 | 每页条数 |
| `keyword` | String | 否 | `@Size(max=64)` | 模糊匹配 `title` / `code` |
| `type` | String | 否 | `STORY/BUG/TASK` | 类型筛选 |
| `priority` | String | 否 | `P0/P1/P2/P3` | 优先级筛选 |
| `status` | String | 否 | 见 `WorkItemStatusEnum` | 状态筛选 |
| `assignee` | String | 否 | — | 负责人筛选 |
| `reporter` | String | 否 | — | 提出人筛选 |
| `sortBy` | String | 否 | 枚举：`createdAt`/`updatedAt`/`priority` | 排序字段；缺省 `createdAt` |
| `sortDir` | String | 否 | `asc`/`desc`；缺省 `desc` | 排序方向 |

> 排序规则：必须显式指定 `sortBy + sortDir`，避免无排序分页（团队规范 §7.1）。

响应：`Result<PageResp<WorkItemRespVO>>`

错误码：`BIZ_PARAM_INVALID`

---

#### 5.1.5 逻辑删除工作项

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `DELETE /api/v1/work-items/{id}` |
| Auth | Y |
| Idempotent | Y（按 `id` 天然幂等） |

路径参数：`id`

响应：`Result<Boolean>`（data = `true`）

错误码：`BIZ_NOT_FOUND` / `BIZ_DONE_IMMUTABLE`

---

### 5.2 状态流转（transition）

#### 5.2.1 触发状态流转

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `POST /api/v1/work-items/{id}/transitions` |
| Auth | Y |
| Idempotent | Y（`Idempotency-Key` 必填） |

路径参数：`id`

请求体 `WorkItemTransitionReqDTO`：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `targetStatus` | String | 是 | `@NotBlank` | 目标状态；必须出现在 `WorkItemStatusEnum` |
| `reason` | String | 否 | `@Size(max=500)` | 变更原因 / 备注 |

响应 `Result<WorkItemTransitionRespVO>`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `workItemId` | Long | 工作项 ID |
| `fromStatus` | String\|null | 变更前状态 |
| `toStatus` | String | 变更后状态 |
| `operator` | String | 操作人（来自 `X-User`） |
| `transitionedAt` | String | 流转时间 |
| `historyId` | Long | 状态历史记录 ID |

邻接表（DONE 不可逆，详见 `StateMachine`）：

```text
DRAFT         → {READY, ARCHIVED}
READY         → {IN_PROGRESS, DRAFT, ARCHIVED}
IN_PROGRESS   → {IN_TESTING, READY, ARCHIVED}
IN_TESTING    → {DONE, IN_PROGRESS, ARCHIVED}
DONE          → {}
ARCHIVED      → {DRAFT}
```

守卫链（按顺序执行，全部通过才允许流转）：

1. `AdjacencyGuard`（内置于状态机）：校验 `(from, to)` 出现在邻接表；否则 `BIZ_TRANSITION_NOT_ALLOWED`。
2. `DoneImmutableGuard`：源或目标为 `DONE` 再次拦截；`BIZ_DONE_IMMUTABLE`。
3. `P0ClarificationGuard`：若 `target ∈ {READY, IN_PROGRESS, IN_TESTING, DONE}` 且 `P0 & OPEN` 计数 > 0 → `BIZ_P0_CLARIFICATION_BLOCKED`。

错误码：`BIZ_PARAM_INVALID` / `BIZ_NOT_FOUND` / `BIZ_TRANSITION_NOT_ALLOWED` / `BIZ_DONE_IMMUTABLE` / `BIZ_P0_CLARIFICATION_BLOCKED`

---

#### 5.2.2 查询状态历史

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `GET /api/v1/work-items/{id}/transitions` |
| Auth | N |
| Idempotent | — |

Query 参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `pageNo` | Integer | 否 | 缺省 1 |
| `pageSize` | Integer | 否 | 缺省 20；最大 100 |
| `toStatus` | String | 否 | 按目标状态过滤 |

响应 `Result<PageResp<WorkItemStatusHistoryRespVO>>`，data.records：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 历史 ID |
| `workItemId` | Long | 工作项 ID |
| `fromStatus` | String\|null | 变更前状态；首建为 null |
| `toStatus` | String | 变更后状态 |
| `reason` | String\|null | 原因 |
| `operator` | String\|null | 操作人 |
| `createdAt` | String | 变更时间 |

错误码：`BIZ_NOT_FOUND`

---

### 5.3 澄清问题（clarification）

#### 5.3.1 新增澄清问题

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `POST /api/v1/work-items/{id}/clarifications` |
| Auth | Y |
| Idempotent | N（依赖业务去重：同 `(workItemId, question)` 不允许） |

请求体 `ClarificationCreateReqDTO`：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `question` | String | 是 | `@NotBlank @Size(max=2000)` | 问题内容 |
| `severity` | String | 否 | `P0/P1/P2`；缺省 `P1` | 严重程度 |
| `raisedBy` | String | 否 | `@Size(max=64)` | 提出人；缺省取 `X-User` |

响应 `Result<ClarificationRespVO>`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键 |
| `workItemId` | Long | 工作项 ID |
| `question` | String | 问题 |
| `severity` | String | 严重程度 |
| `status` | String | 始终为 `OPEN` |
| `answer` | String\|null | 回答（新增时为 null） |
| `raisedBy` | String\|null | 提出人 |
| `resolvedBy` | String\|null | 解决人（新增时为 null） |
| `createdAt` | String | 提出时间 |
| `resolvedAt` | String\|null | 解决时间（新增时为 null） |

错误码：`BIZ_PARAM_INVALID` / `BIZ_NOT_FOUND`（工作项不存在） / `BIZ_DUPLICATE_QUESTION`

---

#### 5.3.2 解决澄清问题

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `PUT /api/v1/clarifications/{cid}` |
| Auth | Y |
| Idempotent | N（解决后再次调用返回 409） |

路径参数：`cid`（Long，澄清问题主键）

请求体 `ClarificationResolveReqDTO`：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `answer` | String | 是 | `@NotBlank @Size(max=2000)` | 解决说明 |
| `resolvedBy` | String | 否 | `@Size(max=64)` | 解决人；缺省取 `X-User` |

响应：`Result<ClarificationRespVO>`

> 业务约束：
> - 仅允许从 `OPEN` → `RESOLVED`；二次解决返回 `BIZ_CLARIFICATION_ALREADY_RESOLVED`。
> - 解决操作**严禁**修改工作项 `status`；状态机守卫通过查询实时反映新计数。

错误码：`BIZ_NOT_FOUND` / `BIZ_PARAM_INVALID` / `BIZ_CLARIFICATION_ALREADY_RESOLVED`

---

#### 5.3.3 澄清问题列表

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `GET /api/v1/work-items/{id}/clarifications` |
| Auth | N |
| Idempotent | — |

Query 参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `severity` | String | 否 | `P0/P1/P2` 过滤 |
| `status` | String | 否 | `OPEN/RESOLVED` 过滤 |
| `pageNo` | Integer | 否 | 缺省 1 |
| `pageSize` | Integer | 否 | 缺省 20；最大 100 |

响应：`Result<PageResp<ClarificationRespVO>>`

错误码：`BIZ_PARAM_INVALID`

---

### 5.4 AI 分析（ai）

#### 5.4.1 触发 AI 分析

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `POST /api/v1/work-items/{id}/ai-analyses` |
| Auth | Y |
| Idempotent | Y（`Idempotency-Key` 必填；5 分钟内同 key 同 type 复用） |

请求体 `AiAnalysisTriggerReqDTO`：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `analysisType` | String | 是 | `@NotBlank` | `SUMMARY/ACCEPTANCE/RISK/CLARIFICATION/TASK_BREAKDOWN` |
| `forceRefresh` | Boolean | 否 | 缺省 false | 是否强制重新分析（绕过幂等复用） |

响应 `Result<AiAnalysisRespVO>`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 分析结果 ID |
| `workItemId` | Long | 工作项 ID |
| `analysisType` | String | 分析类型 |
| `source` | String | `MOCK` / `LLM` |
| `payload` | Object | 结构化结果（key-value 列表，详见 §8.5） |
| `summary` | String | 一句话摘要（payload 渲染） |
| `createdAt` | String | 生成时间 |

错误码：`BIZ_PARAM_INVALID` / `BIZ_NOT_FOUND` / `BIZ_AI_CAPABILITY_NOT_FOUND` / `BIZ_AI_SCHEMA_INVALID` / `BIZ_AI_UPSTREAM_FAILURE`

---

#### 5.4.2 AI 分析历史

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `GET /api/v1/work-items/{id}/ai-analyses` |
| Auth | N |
| Idempotent | — |

Query 参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `analysisType` | String | 否 | 过滤 |
| `source` | String | 否 | `MOCK/LLM` 过滤 |
| `pageNo` | Integer | 否 | 缺省 1 |
| `pageSize` | Integer | 否 | 缺省 10；最大 50 |

响应：`Result<PageResp<AiAnalysisRespVO>>`

错误码：`BIZ_PARAM_INVALID`

---

### 5.5 字典（dict）

#### 5.5.1 查询字典

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `GET /api/v1/dicts` |
| Auth | N |
| Idempotent | — |

Query 参数：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `type` | String | 是 | 字典类型，如 `WORK_ITEM_STATUS` / `WORK_ITEM_TYPE` / `WORK_ITEM_PRIORITY` |
| `enabledOnly` | Boolean | 否 | 缺省 `true`；`false` 返回禁用项 |

响应 `Result<List<DictRespVO>>`，data 元素：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键 |
| `type` | String | 字典类型 |
| `key` | String | 字典项 key |
| `label` | String | 展示名 |
| `value` | String\|null | 业务值 |
| `sort` | Integer | 排序 |
| `enabled` | Boolean | 是否启用 |

错误码：`BIZ_PARAM_INVALID`

---

#### 5.5.2 创建字典项（admin）

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `POST /api/v1/dicts` |
| Auth | A（`X-Role: admin`） |
| Idempotent | N |

请求体 `DictCreateReqDTO`：

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `type` | String | 是 | `@NotBlank @Size(max=32)` | 字典类型 |
| `key` | String | 是 | `@NotBlank @Size(max=32)` | 字典 key |
| `label` | String | 是 | `@NotBlank @Size(max=64)` | 展示名 |
| `value` | String | 否 | `@Size(max=255)` | 业务值 |
| `sort` | Integer | 否 | `@Min(0)`；缺省 0 | 排序 |
| `enabled` | Boolean | 否 | 缺省 true | 是否启用 |

响应：`Result<DictRespVO>`

错误码：`BIZ_PARAM_INVALID` / `BIZ_DUPLICATE_DICT_KEY` / `BIZ_FORBIDDEN`

---

#### 5.5.3 修改字典项（admin）

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `PUT /api/v1/dicts/{id}` |
| Auth | A |
| Idempotent | N |

路径参数：`id`（Long）

请求体 `DictUpdateReqDTO`：

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| `label` | String | 否 | `@Size(max=64)` |
| `value` | String | 否 | `@Size(max=255)` |
| `sort` | Integer | 否 | `@Min(0)` |
| `enabled` | Boolean | 否 | — |

响应：`Result<DictRespVO>`

错误码：`BIZ_NOT_FOUND` / `BIZ_PARAM_INVALID` / `BIZ_FORBIDDEN`

---

#### 5.5.4 删除字典项（admin）

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `DELETE /api/v1/dicts/{id}` |
| Auth | A |
| Idempotent | Y（删除后再次调用返回 `BIZ_NOT_FOUND`，幂等视为成功） |

响应：`Result<Boolean>`（data = `true`）

错误码：`BIZ_NOT_FOUND` / `BIZ_DICT_IN_USE` / `BIZ_FORBIDDEN`

---

### 5.6 健康检查

| 项 | 内容 |
|---|---|
| 方法 / 路径 | `GET /actuator/health` |
| Auth | N |
| Idempotent | — |

响应：Spring Boot Actuator 标准结构（不经 `Result` 包装）：

```json
{ "status": "UP" }
```

---

## 6. 错误码字典

> 与 `ErrorCode` 枚举严格一致；变更需同步代码、PR 评审卡口。

| 错误码 | 含义 | HTTP 状态 | 触发场景 |
|---|---|---|---|
| `BIZ_PARAM_INVALID` | 参数校验失败 | 400 | `@Valid` 失败、JSON 不可读、枚举值非法 |
| `BIZ_NOT_FOUND` | 资源不存在 | 404 | 工作项 / 澄清问题 / 字典项 / AI 结果不存在 |
| `BIZ_DUPLICATE_CODE` | 业务编码重复 | 409 | 创建工作项时 `code` 已存在 |
| `BIZ_DUPLICATE_QUESTION` | 同一工作项下重复问题 | 409 | 澄清问题 `(work_item_id, question)` 已存在 |
| `BIZ_DUPLICATE_DICT_KEY` | 字典 `(type, key)` 重复 | 409 | 字典创建时违反唯一约束 |
| `BIZ_VERSION_CONFLICT` | 乐观锁冲突 | 409 | 修改工作项时 `version` 不匹配 |
| `BIZ_TRANSITION_NOT_ALLOWED` | 状态流转不合法 | 409 | 邻接表外的跳级流转 |
| `BIZ_DONE_IMMUTABLE` | DONE 不可变更 | 409 | DONE 源/目标 / DONE 上修改工作项 / 逻辑删除 DONE |
| `BIZ_P0_CLARIFICATION_BLOCKED` | P0 未解决阻断流转 | 409 | 进入 `READY/IN_PROGRESS/IN_TESTING/DONE` 仍有 P0 OPEN |
| `BIZ_CLARIFICATION_ALREADY_RESOLVED` | 澄清已解决 | 409 | 二次解决 |
| `BIZ_DICT_IN_USE` | 字典被引用 | 409 | 存在引用时删除字典项 |
| `BIZ_FORBIDDEN` | 权限不足 | 403 | 非 admin 调用管理接口 |
| `BIZ_AI_CAPABILITY_NOT_FOUND` | 不支持的 AI 分析类型 | 400 | `analysisType` 不在枚举内 |
| `BIZ_AI_SCHEMA_INVALID` | AI 返回结果不符合 schema | 500 | payload 缺失关键 key |
| `BIZ_AI_UPSTREAM_FAILURE` | 上游 LLM 不可用 | 502 | LLM 模式调用失败 / 超时 |
| `SYS_INTERNAL` | 系统异常 | 500 | 未捕获的 RuntimeException |

错误响应示例：

```json
{
  "code": 409,
  "message": "存在 1 条未解决的 P0 澄清问题，无法进入 READY",
  "data": null,
  "timestamp": 1710000000000
}
```

---

## 7. 幂等

### 7.1 幂等策略

| 维度 | 方案 |
|---|---|
| 标识 | 请求头 `Idempotency-Key: <uuid-or-biz-key>` |
| 存储 | 内存 `IdempotencyStore`（ConcurrentHashMap，TTL 5 分钟） |
| 范围 | 仅写接口（见各接口 Idempotent 列） |
| 命中 | 同 key 同 request-hash → 直接返回上次结果；不重执行业务 |
| 冲突 | 同 key 不同 request-hash → `BIZ_IDEMPOTENCY_CONFLICT` |

### 7.2 必带幂等键的接口

- `POST /api/v1/work-items`
- `POST /api/v1/work-items/{id}/transitions`
- `POST /api/v1/work-items/{id}/ai-analyses`

### 7.3 天然幂等的接口

- `GET` 系列
- `PUT /api/v1/work-items/{id}`（依赖乐观锁）
- `PUT /api/v1/clarifications/{cid}`（业务上二次解决会被拒绝，幂等交由业务层）
- `DELETE` 系列

### 7.4 错误码补充

| 错误码 | 含义 | HTTP |
|---|---|---|
| `BIZ_IDEMPOTENCY_CONFLICT` | 同 key 不同请求体 | 409 |
| `BIZ_IDEMPOTENCY_MISSING` | 必带幂等键的接口未携带 | 400 |

---

## 8. 示例

### 8.1 创建工作项

**请求**

```http
POST /api/v1/work-items HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-User: alice
X-Request-Id: req-001
Idempotency-Key: 7f3c2c8a-1f7d-4d2c-8a4b-1c2d3e4f5a6b
```

```json
{
  "title": "用户登录失败时前端兜底",
  "description": "登录接口 5xx 时，前端需在 2s 内展示友好提示并支持重试。",
  "type": "STORY",
  "priority": "P1",
  "assignee": "bob",
  "tags": ["login", "frontend", "ux"],
  "acceptanceCriteria": [
    "5xx 时 2s 内展示 toast",
    "提供「重试」按钮，点击重新发起登录",
    "重试不产生重复请求（依赖幂等键）"
  ]
}
```

**响应 200**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1001,
    "code": "WI-20260608-0001",
    "title": "用户登录失败时前端兜底",
    "description": "登录接口 5xx 时，前端需在 2s 内展示友好提示并支持重试。",
    "type": "STORY",
    "priority": "P1",
    "status": "DRAFT",
    "riskLevel": null,
    "assignee": "bob",
    "reporter": "alice",
    "tags": ["login", "frontend", "ux"],
    "acceptanceCriteria": [
      "5xx 时 2s 内展示 toast",
      "提供「重试」按钮，点击重新发起登录",
      "重试不产生重复请求（依赖幂等键）"
    ],
    "version": 0,
    "createdAt": "2026-06-08 10:00:00",
    "updatedAt": "2026-06-08 10:00:00"
  },
  "timestamp": 1717828800000
}
```

---

### 8.2 触发状态流转（P0 阻断示例）

**前置**：工作项 `1001` 已存在一条 P0 OPEN 澄清问题。

**请求**

```http
POST /api/v1/work-items/1001/transitions HTTP/1.1
Host: localhost:8080
X-User: bob
Idempotency-Key: t-1001-to-ready-1
Content-Type: application/json
```

```json
{ "targetStatus": "READY", "reason": "需求清晰，准备进入开发" }
```

**响应 409（P0 阻断）**

```json
{
  "code": 409,
  "message": "存在 1 条未解决的 P0 澄清问题，无法进入 READY",
  "data": null,
  "timestamp": 1717828900000
}
```

**解决 P0 后再次流转 → 200**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "workItemId": 1001,
    "fromStatus": "DRAFT",
    "toStatus": "READY",
    "operator": "bob",
    "transitionedAt": "2026-06-08 10:05:00",
    "historyId": 5001
  },
  "timestamp": 1717829100000
}
```

---

### 8.3 触发 AI 分析

**请求**

```http
POST /api/v1/work-items/1001/ai-analyses HTTP/1.1
Host: localhost:8080
X-User: bob
Idempotency-Key: ai-sum-1001-1
Content-Type: application/json
```

```json
{ "analysisType": "SUMMARY" }
```

**响应 200**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 9001,
    "workItemId": 1001,
    "analysisType": "SUMMARY",
    "source": "MOCK",
    "summary": "本工作项关注登录失败时的前端兜底体验，要求 2s 内反馈并支持幂等重试。",
    "payload": {
      "headline": "登录失败前端兜底",
      "background": "登录 5xx 时缺乏用户反馈",
      "goal": "2s 内反馈 + 幂等重试",
      "scope": ["前端 toast", "重试按钮", "幂等键"],
      "risks": ["重试引发重复登录", "5xx 频次未知"],
      "keyPoints": [
        "统一 request 拦截器捕获 5xx",
        "重试按钮复用 Idempotency-Key",
        "toast 文案与设计稿对齐"
      ]
    },
    "createdAt": "2026-06-08 10:10:00"
  },
  "timestamp": 1717829400000
}
```

---

### 8.4 AI Payload 通用结构

> 详见 §9.5。每种 `analysisType` 对应固定 key 集合，由代码侧 schema 校验。

```jsonc
{
  "SUMMARY": {
    "headline": "String  ≤64",
    "background": "String  ≤500",
    "goal": "String  ≤255",
    "scope": ["String"],
    "risks": ["String"],
    "keyPoints": ["String"]
  },
  "ACCEPTANCE": {
    "criteria": [
      { "given": "String", "when": "String", "then": "String" }
    ],
    "coverage": "String  描述覆盖维度"
  },
  "RISK": {
    "level": "LOW|MEDIUM|HIGH",
    "items": [
      { "type": "String", "desc": "String", "mitigation": "String" }
    ]
  },
  "CLARIFICATION": {
    "questions": [
      { "question": "String", "severity": "P0|P1|P2", "reason": "String" }
    ]
  },
  "TASK_BREAKDOWN": {
    "tasks": [
      { "title": "String", "estimateHours": 1.5, "ownerHint": "frontend|backend|qa" }
    ],
    "totalEstimateHours": 8.0
  }
}
```

---

### 8.5 字典查询

**请求**

```http
GET /api/v1/dicts?type=WORK_ITEM_STATUS HTTP/1.1
Host: localhost:8080
```

**响应 200**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "id": 1, "type": "WORK_ITEM_STATUS", "key": "DRAFT",       "label": "草稿",     "value": null, "sort": 10, "enabled": true },
    { "id": 2, "type": "WORK_ITEM_STATUS", "key": "READY",       "label": "待开发",   "value": null, "sort": 20, "enabled": true },
    { "id": 3, "type": "WORK_ITEM_STATUS", "key": "IN_PROGRESS", "label": "开发中",   "value": null, "sort": 30, "enabled": true },
    { "id": 4, "type": "WORK_ITEM_STATUS", "key": "IN_TESTING",  "label": "测试中",   "value": null, "sort": 40, "enabled": true },
    { "id": 5, "type": "WORK_ITEM_STATUS", "key": "DONE",        "label": "已完成",   "value": null, "sort": 50, "enabled": true },
    { "id": 6, "type": "WORK_ITEM_STATUS", "key": "ARCHIVED",    "label": "已归档",   "value": null, "sort": 90, "enabled": true }
  ],
  "timestamp": 1717829500000
}
```

---

## 9. 附录

### 9.1 完整 API 速查

| 模块 | Method | Path | Auth | Idempotent | 说明 |
|---|---|---|---|---|---|
| workitem | POST | `/api/v1/work-items` | Y | Y | 创建 |
| workitem | PUT | `/api/v1/work-items/{id}` | Y | N | 修改（乐观锁） |
| workitem | GET | `/api/v1/work-items/{id}` | N | — | 详情 |
| workitem | GET | `/api/v1/work-items` | N | — | 分页查询 |
| workitem | DELETE | `/api/v1/work-items/{id}` | Y | Y | 逻辑删除 |
| transition | POST | `/api/v1/work-items/{id}/transitions` | Y | Y | 状态流转 |
| transition | GET | `/api/v1/work-items/{id}/transitions` | N | — | 状态历史 |
| clarification | POST | `/api/v1/work-items/{id}/clarifications` | Y | N | 新增澄清 |
| clarification | PUT | `/api/v1/clarifications/{cid}` | Y | N | 解决澄清 |
| clarification | GET | `/api/v1/work-items/{id}/clarifications` | N | — | 澄清列表 |
| ai | POST | `/api/v1/work-items/{id}/ai-analyses` | Y | Y | 触发 AI |
| ai | GET | `/api/v1/work-items/{id}/ai-analyses` | N | — | AI 历史 |
| dict | GET | `/api/v1/dicts` | N | — | 字典查询 |
| dict | POST | `/api/v1/dicts` | A | N | 创建字典项 |
| dict | PUT | `/api/v1/dicts/{id}` | A | N | 修改字典项 |
| dict | DELETE | `/api/v1/dicts/{id}` | A | Y | 删除字典项 |
| health | GET | `/actuator/health` | N | — | 健康检查 |

### 9.2 关键不变量（接口层面）

1. `Result.code === 0` 为成功；非 0 前端应读取 `message` 提示用户。
2. 分页接口 `data` 恒为 `PageResp<T>`：`{ pageNo, pageSize, total, records }`。
3. 列表查询**必须**显式排序；未指定时服务端按 `createdAt DESC`。
4. `X-User` 缺省时写接口仍允许（记录 `anonymous`）；管理接口需 `X-Role: admin`。
5. 状态流转永远伴随一条 `work_item_status_history` 写入（同一事务）。
6. 解决澄清问题不修改工作项 `status`；状态机守卫通过查询实时放行。
7. AI 返回的 `payload` 强 schema 校验；缺失关键 key 一律不入库。
8. 所有时间字段出参格式 `yyyy-MM-dd HH:mm:ss`；入参宽容 `yyyy-MM-ddTHH:mm:ssZ`。

### 9.3 前后端协作约定

- 前端**统一**通过 `src/api/*.js` 封装；不得在页面中直接 `axios`。
- 拦截器：拿到响应后若 `code !== 0`，统一 toast `message` 并 reject。
- 列表 / 详情 / 弹窗三处复用：`WorkItemList` / `WorkItemDetail` / `TransitionDialog` / `ClarificationDialog` / `AiAnalysisDialog`。
- 字典数据：前端按 `type` 缓存 5 分钟，缺省从枚举内置 `desc` 兜底。
- 错误码翻译：前端维护一份 `errorCodeMap.json`，按 `code` 展示精细化提示（如 `BIZ_P0_CLARIFICATION_BLOCKED` 引导「先去解决 P0」）。

### 9.4 后续扩展

| 优先级 | 能力 | 影响面 |
|---|---|---|
| P1 | 引入 JWT 鉴权，替换 `X-User` 直传 | 新增 `AuthFilter`；UserContext 字段扩展 |
| P1 | `OpenAPI 3` 文档（springdoc）自动生成并部署 | 配置 `springdoc.swagger-ui.path=/swagger-ui.html` |
| P2 | 工作项搜索（ES / 简单全文） | 新增 `/work-items/search` |
| P2 | 状态流转异步通知（WebSocket / SSE） | 新增 `/work-items/{id}/transitions/stream` |
| P2 | AI 异步任务 + 轮询 | `trigger` 返回 `taskId`，新增 `/ai-analyses/{taskId}` |
| P3 | 批量操作（批量流转、批量解决澄清） | 新增 `POST /work-items/batch/...` |
| P3 | 审计日志接口 | 新增 `/audit-logs` 模块 |
| P3 | 多租户隔离 | 全部列表接口增加 `tenantId` 过滤 |

### 9.5 AI Payload 强 Schema 说明

- 每种 `analysisType` 在代码侧 `AiAnalysisTypeEnum` 注册必填 key 集合。
- `AiAnalysisService` 在写入 `ai_analysis_result.payload` 前调用 schema 校验器：
  - 缺失必填 key → 抛 `BIZ_AI_SCHEMA_INVALID`，不回写数据库；
  - 类型不匹配 → 同上；
  - 字段长度超限 → 同上并 WARN 日志。
- MOCK 适配器基于规则模板生成符合 schema 的固定结构；LLM 适配器走 prompt 约束 + 后置 schema 校验。
- 后续增加新类型时，**必须**同步更新本节。

### 9.6 关联文档索引

- 系统设计：[./architecture.md](./architecture.md)
- 需求理解：[../requirements/requirement.md](../requirements/requirement.md)
- 任务拆解：[../tasks/breakdown.md](../tasks/breakdown.md)
- 数据库脚本：[../database/db.sql](../database/db.sql)
- 编码规范：[../../.trae/rules/group_development_rule.md](../../.trae/rules/group_development_rule.md)
- API 设计模板：[../templates/api-design-proposal.md](../templates/api-design-proposal.md)
- 过程记录：[../process.md](../process.md)
- AI 使用记录：[../ai-usage.md](../ai-usage.md)

---

> 本契约与 `architecture.md` 强一致：邻接表、守卫链、AI 能力、错误码、字典类型任一变更需同步更新本文件。
> PR 评审卡口：接口路径、错误码、字段命名变更必须在本文件留痕。
