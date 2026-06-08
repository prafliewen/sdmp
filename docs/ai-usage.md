# AI 使用说明

> 文档目的：完整记录本项目开发过程中的 AI 工具使用情况、AI 生成内容与人工修正，符合 [提交规范](./02_提交规范.md) §4 要求。
> 适用对象：面试评审、协作研发、未来的我。

---

## 1. 使用的 AI 工具

| 工具 | 用途 |
|---|---|
| Trae IDE（内置 DeepSeek-V4 等模型） | 需求理解、任务拆解、方案设计、代码生成、测试生成、文档编写、Bug 修复 |

> 本项目所有 AI 辅助均通过 Trae IDE 内置的 AI 编程助手完成，未使用其他外部 AI 工具。

---

## 2. 使用场景

| 阶段 | 是否使用 AI | 说明 |
|---|---|---|
| 需求理解 | 是 | 阅读 `business_sences.md` / `candidate_backend.md` / 团队编码规范，输出 `requirement.md` 业务目标 / 角色 / 流程 / 范围 / NFR / 风险 / 待确认问题 |
| 任务拆解 | 是 | 按"工程基础 → 公共组件 → 数据 → workitem → state machine → clarification → ai → dict → 前端 → 测试 → 文档"分解为 11 个里程碑（M0-M10） |
| 方案设计 | 是 | 状态机邻接表、守卫链接口签名、AI Capability/Adapter 三层抽象、错误码字典、`Result<T>` 统一返回体、API 契约、数据模型设计 |
| 代码生成 | 是 | 公共层（Result / PageResp / ErrorCode / BizException / UserContext / GlobalExceptionHandler）、实体 / DTO / VO / Mapper 接口 / Service 接口、各 Service 实现、Controller、StateMachine、P0ClarificationGuard、MockAiAdapter、3 个 AiCapability 实现、前端 Vue 页面（WorkItemList / WorkItemDetail） |
| 测试生成 | 是 | 后端 16 个测试类（单元 + Controller 集成）：StateMachineTest / P0ClarificationGuardTest / MockAiAdapterTest / 各模块 Service/Controller 测试；前端 8 个 vitest 测试文件 |
| Bug 修复 | 是 | 状态机执行顺序（DONE 不可变提前校验）、事务原子性（状态变更与历史写入合并）、软删除与 DONE 不可删冲突、前端 request.js unwrap 层级、AI 能力路由 switch → List 注入 |
| 文档编写 | 是 | architecture.md / api-design.md / breakdown.md / db.sql / process.md / README.md / 本 ai-usage.md |

---

## 3. 关键 Prompt / Skill 摘要

> 以下按阶段摘要描述 AI 助手的核心输入与输出，不要求完整对话。

### 3.1 需求理解阶段

**输入**：要求 AI 阅读需求文档和编码规范，输出结构化需求理解。

```
深度理解 docs/requirements/business_sences.md 和 docs/requirements/candidate_backend.md，
结合 .trae/rules/group_development_rule.md 团队规范，
输出结构化需求理解到 docs/requirements/requirement.md，
包含：业务目标、角色、流程、范围、非功能要求、风险、待确认问题、验收标准。
```

**AI 产出**：`requirement.md` 全文骨架，包括 10 个章节（业务目标 / 角色 / 核心概念 / 流程 / 范围 / NFR / 待确认 / 验收 / 风险 / 前置条件）。

### 3.2 任务拆解阶段

**输入**：要求 AI 基于需求文档拆解为可执行的里程碑。

```
基于 docs/requirements/requirement.md，输出任务拆解文档到 docs/tasks/breakdown.md，
拆解为：工程基础 → 公共组件 → 数据模型 → workitem → state machine → clarification → ai → dict → 前端 → 测试 → 文档。
```

**AI 产出**：`breakdown.md` 11 个里程碑的 Checkbox 任务清单。

### 3.3 方案设计阶段

**输入**：要求 AI 设计状态机、守卫链、AI 能力抽象和错误码体系。

```
设计 6 态状态机（DRAFT/ANALYZING/READY/IN_PROGRESS/IN_TESTING/DONE），
用邻接表声明合法流转，DONE 终态不可变。
设计守卫链模式：WorkItemTransitionGuard 接口 + Spring List 注入，
P0ClarificationGuard 在 P0 OPEN > 0 时阻断 READY 及之后状态。
设计 AI 能力三层抽象：AiCapability（业务侧 / supports + analyse）→ AiAdapter（模型侧 / execute）→ MockAiAdapter。
设计统一错误码体系：业务码 BIZ_* 与系统码 SYS_INTERNAL 分离。
```

**AI 产出**：`architecture.md` 中的状态机图、守卫链签名、AI 三层抽象接口定义、16 个 ErrorCode 字典。

### 3.4 代码生成阶段

**输入**：要求 AI 按规范逐步生成后端代码。

```
按 group_development_rule.md 规范，为 WorkItem 模块生成完整分层代码：
Entity / DTO / VO / Mapper / Service 接口 / Service 实现 / Controller，
Controller 只做参数接收与 @Valid 校验，
Service 加 @Transactional，
Mapper 用 MyBatis-Plus LambdaQueryWrapper，
统一返回 Result<T>。
```

类似 Prompt 用于 clarification / ai / dict 模块，以及前端 Vue 页面。

### 3.5 测试生成阶段

**输入**：要求 AI 生成核心业务规则的单测和集成测试。

```
为 StateMachine 生成单元测试，覆盖：
- 6 态全部合法流转
- 非法跨态流转（如 DRAFT → IN_PROGRESS）
- 回退流转（如 IN_PROGRESS → READY）
- DONE 终态不可变（任意 from/to DONE 被拒）
```

类似 Prompt 用于守卫、Mock AI、各 Service/Controller 测试。

### 3.6 Bug 修复阶段

**输入**：报告具体问题，要求 AI 定位并修复。

```
StateMachine 中 DONE 不可变的校验在守卫链里执行，但守卫链排在邻接表校验之后。
结果：从 DONE 流转时先过邻接表（DONE 无邻接目标 → BIZ_TRANSITION_NOT_ALLOWED），
再被守卫拦截（→ BIZ_DONE_IMMUTABLE），双重报错且语义混乱。
修复：把 DONE 不可变提到 StateMachine.assertTransit 中单独判，且先于邻接表。
```

```
WorkItemTransitionServiceImpl.transit 中状态更新与历史写入分两次方法调用，未加 @Transactional。
修复：合并到同一个事务方法，workItemMapper.updateById 后紧跟 historyMapper.insert。
```

---

## 4. AI 生成内容

### 4.1 文档（AI 生成骨架，人工修正与代码对齐）

| 文档 | AI 贡献 | 人工修正 |
|---|---|---|
| `docs/requirements/requirement.md` | 全文 10 章节骨架 | 优先级默认值 P0~P3、状态集合 6 态、P0 阻断边界定义 |
| `docs/tasks/breakdown.md` | 11 个里程碑 Checkbox | 调整为 workitem → clarification → ai → dict 顺序 |
| `docs/design/architecture.md` | 分层架构图、状态机图 | 邻接表回退方向（仅相邻）、DONE 不可变守卫位置 |
| `docs/design/api-design.md` | 全部 API 契约 + 错误码 | 与代码严格对齐字段名、路径、状态码 |
| `docs/process.md` | 全文 8 章节骨架 | 修正问题描述、补充真实修正方式 |
| `docs/database/db.sql` | 5 张表 DDL + 种子数据 | 索引（`work_item_id, severity, status`）、字段类型微调 |
| `README.md` | 全文 10 章节 | 与代码对齐所有内容 |

### 4.2 后端代码（AI 生成初版，人工修正后可用）

| 模块 | 文件 | AI 贡献 |
|---|---|---|
| 公共层 | `Result.java` / `PageResp.java` / `ErrorCode.java` / `BizException.java` / `GlobalExceptionHandler.java` | 全部初版 |
| 用户上下文 | `UserContext.java` / `UserContextFilter.java` | 全部初版 |
| 配置 | `MybatisPlusConfig.java` / `MyMetaObjectHandler.java` | 全部初版（需修正乐观锁拦截器） |
| 实体 | `WorkItemEntity.java` / `WorkItemStatusHistoryEntity.java` / `ClarificationQuestionEntity.java` / `AiAnalysisResultEntity.java` / `DictItemEntity.java` | 全部初版 |
| 枚举 | 8 个枚举类 | 全部初版 |
| 状态机 | `StateMachine.java` | 全部初版（需修正 DONE 校验顺序） |
| 守卫 | `WorkItemTransitionGuard.java` / `P0ClarificationGuard.java` | 全部初版 |
| DTO/VO | 所有 ReqDTO / RespVO | 全部初版 |
| Mapper | 5 个 Mapper 接口 | 全部初版 |
| Service | 所有 Service 接口 + Impl（WorkItem / Transition / Clarification / AI / Dict） | 全部初版 |
| Controller | 5 个 Controller | 全部初版 |
| AI 层 | `AiCapability.java` / `AiAdapter.java` / `MockAiAdapter.java` / `SummaryCapability.java` / `RiskCapability.java` / `ClarificationCapability.java` | 全部初版 |

### 4.3 前端代码（AI 生成初版）

| 文件 | AI 贡献 |
|---|---|
| `utils/request.js` | 全部初版（需修正 unwrap 层级和错误拦截） |
| `api/workitem.js` / `transition.js` / `clarification.js` / `ai.js` / `dict.js` | 全部初版 |
| `store/user.js` | 全部初版 |
| `router/index.js` | 全部初版 |
| `views/WorkItemList.vue` | 全部初版（筛选栏、表格、分页、创建弹窗） |
| `views/WorkItemDetail.vue` | 全部初版（基本信息卡、4 个 Tab、3 种弹窗） |
| `App.vue` / `main.js` | 全部初版 |

### 4.4 测试代码（AI 生成骨架）

| 文件 | AI 贡献 |
|---|---|
| 后端 16 个测试类 | 全部骨架（需补 @Mock / @InjectMocks 和边界用例） |
| 前端 8 个 vitest 测试文件 | 全部骨架 |

---

## 5. 人工修正内容

### 5.1 必须修正（不修正则功能异常）

| 问题 | AI 原始行为 | 修正方式 |
|---|---|---|
| MyBatis-Plus `@Version` 乐观锁不生效 | AI 只在 Entity 加了 `@Version` 注解，未注册拦截器 | 在 `MybatisPlusConfig` 中显式注册 `OptimisticLockerInnerInterceptor` |
| 状态变更与历史写入非原子 | AI 把 `update(work_item)` 和 `insert(history)` 拆成两次调用且无事务 | 合并到 `WorkItemTransitionServiceImpl.transit` 并加 `@Transactional` |
| DONE 不可变校验位置错误 | AI 放在守卫链中，排在邻接表之后，导致双重报错 | 提到 `StateMachine.assertTransit` 单独判，且先于邻接表 |
| 软删除绕过 DONE 不可删 | AI 未在 `delete` 方法中检查 DONE 状态，`@TableLogic` 直接逻辑删除 | 在 `WorkItemServiceImpl.delete` 中先查状态，DONE 抛 `BIZ_DONE_IMMUTABLE` |
| 前端 `res.data.data` 双重嵌套 | AI 在调用端写了 `res.data.records`，拦截器已 unwrap 一层 | 统一改为 `res.records`，并在 `request.js` 顶部注释说明 unwrap 规则 |
| AI 能力路由用 `switch-case` | AI 在 `AiAnalysisServiceImpl` 里写 `switch(type)` 分发 | 改为 `List<AiCapability>` 注入 + `supports()` 路由 |
| P0 守卫无数据库索引 | AI 生成的建表 SQL 缺少 `(work_item_id, severity, status)` 索引 | 在 `db.sql` 中补复合索引 |
| 流转 API 缺历史分页查询 | AI 未生成 `GET /work-items/{id}/transitions` 接口 | 补 `WorkItemTransitionController.listHistory` |

### 5.2 调整优化（不改也能用，但不符合规范）

| 问题 | 修正方式 |
|---|---|
| `@TableLogic` 默认值行为不确定 | 显式配置 `db.config.logic-delete-field: deleted` |
| 任务拆解的实施顺序不匹配用户偏好 | 调整为 workitem → clarification → ai → dict |
| 邻接表回退方向过于宽松（AI 默认允许跨态回退） | 限制为仅相邻回退 |
| 前端错误拦截缺少网络异常分支 | 在 `request.js` 补充 `error.response` 不存在时的兜底 message |
| 测试用例边界不完整 | 增加 DONE 不可变、P0 计数为 0 放行、重复解决、乐观锁冲突等边界用例 |

### 5.3 已知未修（已记录，低优先级）

| 项 | 位置 | 原因 |
|---|---|---|
| `AcceptanceCapability` / `TaskBreakdownCapability` 实现类 | `ai/capability/` | 枚举与 Mock payload 就绪，5 分钟可补 |
| 前端字典 label 展示 | `WorkItemDetail.vue` / `WorkItemList.vue` | 字典数据已暴露，硬编码 `statusLabel` 待替换 |
| 详情页 `p0OpenClarifications` 真实计数 | `WorkItemServiceImpl.getWorkItemDetail` | 当前返回 0，注入 `ClarificationService.countP0Open(id)` 即可修复 |

---

## 6. 效果评价

### 6.1 提效显著（✅）

| 场景 | 说明 |
|---|---|
| 需求文档结构化 | AI 将零散的需求文档（`business_sences.md` / `candidate_backend.md`）解析为 `requirement.md` 的 10 章节完整结构化输出，包含业务目标、角色、流程、NFR、风险、待确认和验收标准，人工只需微调默认假设 |
| 状态机 / 守卫链代码 | 6 态邻接表、DONE 不可变校验、P0 守卫链的初版代码由 AI 完成，Spring 注入 `List<Guard>` 自动排序的模式 AI 直接给出了正确实现 |
| AI 能力三层抽象 | `AiCapability`（业务侧）/ `AiAdapter`（模型侧）/ `MockAiAdapter`（实现） 的三层抽象初版代码结构合理，`List<AiCapability>` 注入 + `supports()` 路由思路正确 |
| 错误码字典 | 16 个 ErrorCode 枚举的完整代码由 AI 一次生成，业务码/系统码分离、message 模板合理 |
| 测试骨架 | 16 个后端测试类 + 8 个前端测试文件的骨架（import、类结构、基础断言）由 AI 批量生成 |
| 文档编写 | architecture / api-design / breakdown / db.sql / README 等文档的骨架大幅节省时间 |
| 前端 Vue 页面 | WorkItemList 和 WorkItemDetail 的模板结构、computed、方法签名由 AI 生成，UI 布局基本可用 |

### 6.2 需复核（⚠️）

| 场景 | 说明 |
|---|---|
| MyBatis-Plus 配置 | AI 对 `@Version` 乐观锁拦截器、`@TableLogic` 默认值、分页插件的理解不完全准确，需要人工对照官方文档校正 |
| 前端 vite proxy 路径 | AI 生成的 `vite.config.js` 代理配置路径有时与实际后端路径不一致 |
| SQL 索引建议 | AI 生成建表 SQL 不会主动推荐复合索引（如 P0 守卫查询用到的 `(work_item_id, severity, status)`），需人工补 |
| 前端错误处理 | AI 生成的 `request.js` 拦截器对 `code !== 0` 的处理逻辑需要人工确认（是否 unwrap data、网络异常分支是否覆盖） |

### 6.3 不能直接用（❌）

| 场景 | 说明 |
|---|---|
| 乐观锁配置 | AI 生成的代码在 Entity 加了 `@Version` 但未在 MyBatis-Plus 配置类中注册 `OptimisticLockerInnerInterceptor`，导致"看似成功实际跳过" |
| 事务边界 | AI 倾向于把跨表操作拆成多个独立方法调用，缺少 `@Transactional` 合并意识，必须人工审计并修正 |
| 并发与边界 | AI 生成的代码在并发更新、空值处理、异常路径上容易遗漏，必须有测试覆盖 |
| 错误码语义 | AI 默认用 HTTP 标准状态码（400/409/500）而非语义化业务码（`BIZ_P0_CLARIFICATION_BLOCKED`），需人工统一替换 |

### 6.4 总体评价

- AI 在**需求理解、文档编写、代码骨架生成、测试骨架生成**方面提效显著，约占开发总量的 60%-70% 初版产出，核心作用是"把重复的结构性工作做了"。
- AI 在**事务边界、并发安全、框架配置细节、边界条件**方面表现不稳定，需要人工逐项审计与修正，这部分约占修改量的 30%-40%。
- **最终可用代码**中，AI 约占 50%-60%（结构、命名、分层、基础逻辑），人工约占 40%-50%（安全、并发、边界、测试覆盖、规范对齐）。
- 建议后续继续使用 AI 作为"结构化产出的第一版"，但必须配合单元测试和 Code Review 把关关键路径。

---

## 附录

- 过程记录：[docs/process.md](./process.md)
- 架构设计：[docs/design/architecture.md](./design/architecture.md)
- API 契约：[docs/design/api-design.md](./design/api-design.md)
- 任务拆解：[docs/tasks/breakdown.md](./tasks/breakdown.md)
- 需求理解：[docs/requirements/requirement.md](./requirements/requirement.md)
- 团队编码规范：[.trae/rules/group_development_rule.md](../.trae/rules/group_development_rule.md)
- 提交规范：[docs/02_提交规范.md](./02_提交规范.md)