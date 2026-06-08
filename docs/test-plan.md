# 测试说明：AI 辅助研发工作项流转与需求澄清系统

> 文档目的：依据 [docs/02_提交规范.md §6 测试说明要求](../02_提交规范.md) 与 [docs/templates/test-plan.md](./templates/test-plan.md) 模板，说明本项目如何验证核心业务规则、状态流转、澄清问题阻断、AI 辅助能力，以及尚未覆盖的风险。
> 适用范围：后端单测 / Controller 集成 / 前端 Vitest。
> 关联文档：
> - 需求理解：[../requirements/requirement.md](../requirements/requirement.md)
> - 系统设计：[../design/architecture.md](../design/architecture.md)
> - API 契约：[../design/api-design.md](../design/api-design.md)
> - 过程记录：[../process.md](../process.md)
> - 任务拆解：[../tasks/breakdown.md](../tasks/breakdown.md)

---

## 1. 测试范围

### 1.1 后端（Java 21 / Spring Boot 3.3 / MyBatis-Plus / JUnit 5 + Mockito）

测试入口：`backend/` 目录，`mvn test` 一键执行。

| 类别 | 测试类 | 覆盖模块 | 覆盖点 |
|---|---|---|---|
| 公共层 | [CommonLayerTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/CommonLayerTest.java) | `Result` / `PageResp` / `BizException` / `ErrorCode` / 8 个枚举 | 统一返回体、错误码常量、枚举解析、非法值抛异常 |
| 状态机 | [StateMachineTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/StateMachineTest.java) | `StateMachine` | 6 态邻接表合法 / 非法 / 回退 / 跨级 / DONE 不可变 |
| 守卫 | [P0ClarificationGuardTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/P0ClarificationGuardTest.java) | `P0ClarificationGuard` | P0 OPEN 计数 > 0 时阻断 `READY/IN_PROGRESS/IN_TESTING/DONE`；计数为 0 或非阻断目标放行 |
| 用户上下文 | [UserContextTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/UserContextTest.java) | `UserContext` ThreadLocal | 设置 / 获取 / 缺省值 / `clear` / 线程隔离 |
| 全局异常 | [GlobalExceptionHandlerTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/GlobalExceptionHandlerTest.java) | `GlobalExceptionHandler` | `BizException` / `MethodArgumentNotValidException` / `HttpMessageNotReadableException` / 未知异常 |
| Mock AI 适配器 | [MockAiAdapterTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/MockAiAdapterTest.java) | `MockAiAdapter` | 5 种 `analysisType` 全部返回非空结构化 payload 且互不污染 |
| WorkItem Service | [WorkItemServiceImplTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/WorkItemServiceImplTest.java) | `WorkItemServiceImpl` | 创建、`code` 自增 / 指定、详情、分页、按状态过滤、更新、乐观锁、软删除、DONE 不可变 |
| WorkItem Controller | [WorkItemControllerIntegrationTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/WorkItemControllerIntegrationTest.java) | `WorkItemController` | `POST/PUT/GET/DELETE /api/v1/work-items[/{id}]` 端到端 + 必填校验 |
| 流转 Service | [WorkItemTransitionServiceImplTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/WorkItemTransitionServiceImplTest.java) | `WorkItemTransitionServiceImpl` | 状态机 + 守卫链 + 历史同事务写入、历史分页、空历史 |
| 流转 Controller | [WorkItemTransitionControllerIntegrationTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/WorkItemTransitionControllerIntegrationTest.java) | `WorkItemTransitionController` | `POST/GET /api/v1/work-items/{id}/transitions` + 缺 `targetStatus` 400 |
| 澄清 Service | [ClarificationServiceImplTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/ClarificationServiceImplTest.java) | `ClarificationServiceImpl` | 新增、重复检测、缺省 P1 严重度、解决、重复解决、不存在、列表、P0 OPEN 计数 |
| 澄清 Controller | [ClarificationControllerIntegrationTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/ClarificationControllerIntegrationTest.java) | `ClarificationController` | `POST/GET /api/v1/work-items/{id}/clarifications` + `PUT /api/v1/clarifications/{cid}` + 缺 `question` 400 |
| AI Service | [AiAnalysisServiceImplTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/AiAnalysisServiceImplTest.java) | `AiAnalysisServiceImpl` | 能力路由（SUMMARY / RISK）、`payload` 持久化、`summary` 抽取（`headline` / `level`）、非法 `analysisType`、空 payload、历史分页 |
| AI Controller | [AiAnalysisControllerIntegrationTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/AiAnalysisControllerIntegrationTest.java) | `AiAnalysisController` | `POST/GET /api/v1/work-items/{id}/ai-analyses` + 缺 `analysisType` 400 |
| Dict Service | [DictServiceImplTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/DictServiceImplTest.java) | `DictServiceImpl` | 列表（仅 enabled / 全部）、空结果、创建、重复 `type+key`、更新、不存在、删除 |
| Dict Controller | [DictControllerIntegrationTest.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/test/java/com/sdpm/workitem/DictControllerIntegrationTest.java) | `DictController` | `GET/POST/PUT/DELETE /api/v1/dicts[/{id}]` + 缺 `type` 400 |

> 共 **16 个测试类**，约 **120+ 个用例**（包含每个测试方法中的多个分支断言）。所有用例均使用 `MockitoExtension`，仅 `WebMvcTest` 走 Spring MVC 切片。

### 1.2 前端（Vue 3 / Vitest）

测试入口：`frontend/` 目录，`npm test` 一键执行。

| 测试文件 | 覆盖模块 |
|---|---|
| `src/api/workitem.test.js` | 工作项 API 封装 |
| `src/api/transition.test.js` | 状态流转 API 封装 |
| `src/api/clarification.test.js` | 澄清问题 API 封装 |
| `src/api/ai.test.js` | AI 分析 API 封装 |
| `src/api/dict.test.js` | 字典 API 封装 |
| `src/utils/request.test.js` | axios 拦截器 / unwrap / 错误 toast |
| `src/store/user.test.js` | `useUserStore`（操作人） |
| `src/views/WorkItemList.test.js` | 列表页渲染 + 筛选 + 分页 + 创建弹窗 |
| `src/views/WorkItemDetail.test.js` | 详情页 + Tab + 弹窗 |

### 1.3 不在测试范围

- 真实数据库集成（后端测试以 Mockito 桩接 Mapper，依赖 `application-test.yml` 启动 Spring 上下文时也使用 H2/MySQL 内存数据源进行 Controller 集成；不涉及真实 MySQL 8 集群）。
- 真实 LLM 调用（AI 路径使用 `MockAiAdapter`）。
- 浏览器端到端（无 Playwright / Cypress）。
- 性能 / 压力 / 并发测试。

---

## 2. 核心业务规则验证

> 全部断言以"实测可重复"为标准；每条规则对应至少 1 个自动化用例 + 1 个手工回归路径（见 §7）。

| # | 规则 | 验证方式 | 关键用例 | 结果 |
|---|---|---|---|---|
| R1 | 工作项创建时若未传 `code`，自动生成 `WI-yyyyMMdd-序号` | 单元：`WorkItemServiceImplTest.shouldAutoGenerateCode` 断言返回值的 `code` 以 `WI-` 开头 | `shouldAutoGenerateCode` / `shouldUseSpecifiedCode` | ✅ |
| R2 | 工作项创建后初始状态为 `DRAFT` | 单元：`WorkItemServiceImplTest.shouldCreateWorkItemSuccessfully` 断言 `result.getStatus() == "DRAFT"`；集成：`WorkItemControllerIntegrationTest` 间接验证 | `shouldCreateWorkItemSuccessfully` | ✅ |
| R3 | 工作项支持 `tags` / `acceptanceCriteria` JSON 序列化 | 单元：`WorkItemServiceImplTest.shouldCreateWorkItemSuccessfully` 入参 `tags=[tag1,tag2]`；详情查询断言反序列化后 List 不为空 | `shouldCreateWorkItemSuccessfully` / `shouldGetDetail` | ✅ |
| R4 | 工作项修改受乐观锁保护，`version` 错配返回 `BIZ_VERSION_CONFLICT` | 单元：`WorkItemServiceImplTest.shouldThrowVersionConflict` mock `updateById` 返回 0 → 抛 `BIZ_VERSION_CONFLICT` | `shouldThrowVersionConflict` | ✅ |
| R5 | 工作项状态为 `DONE` 时不可修改 / 不可软删除 | 单元：`WorkItemServiceImplTest.shouldRejectUpdateDone` / `shouldRejectDeleteDone` | 同名用例 | ✅ |
| R6 | 6 态显式状态机：`DRAFT → ANALYZING → READY → IN_PROGRESS → IN_TESTING → DONE`，DONE 为终态 | 单元：`StateMachineTest` 12 个用例覆盖合法 / 非法 / 回退 / 跨级 / 终态 | 见 §3 | ✅ |
| R7 | 仅允许相邻回退（`ANALYZING → DRAFT` / `IN_PROGRESS → READY` / `IN_TESTING → IN_PROGRESS`） | 单元：`StateMachineTest.shouldAllowAnalyzingToDraft` / `shouldAllowInTestingToInProgress` / `shouldRejectInProgressToDraft` | 同名用例 | ✅ |
| R8 | 进入 `READY/IN_PROGRESS/IN_TESTING/DONE` 任意目标时，若存在 `severity=P0 AND status=OPEN` 澄清问题，抛 `BIZ_P0_CLARIFICATION_BLOCKED`，`message` 携带 P0 计数 | 单元：`P0ClarificationGuardTest` 6 个用例 | 见 §4 | ✅ |
| R9 | 进入 `DRAFT` / `ANALYZING` 不触发 P0 拦截（非阻断目标） | 单元：`P0ClarificationGuardTest.shouldPassForNonBlockedStatus` | 同名用例 | ✅ |
| R10 | 解决澄清问题不修改工作项 `status`（状态机守卫每次实时查询） | 单元：`WorkItemTransitionServiceImplTest.shouldBeBlockedByGuard` 验证守卫链按声明顺序执行；`ClarificationServiceImplTest.shouldResolveQuestion` 验证解决后 `status=RESOLVED` 且工作项 `status` 不变 | `shouldResolveQuestion` / `shouldBeBlockedByGuard` | ✅ |
| R11 | 同工作项下 `question` 唯一 | 单元：`ClarificationServiceImplTest.shouldRejectDuplicateQuestion` | 同名用例 | ✅ |
| R12 | 重复解决同一澄清问题抛 `BIZ_CLARIFICATION_ALREADY_RESOLVED` | 单元：`ClarificationServiceImplTest.shouldRejectDoubleResolve` | 同名用例 | ✅ |
| R13 | 5 种 AI 能力全部按 `AiAnalysisTypeEnum` 注册并返回**结构化** payload（key-value Map，非散文） | 单元：`MockAiAdapterTest` 6 个用例 | 见 §5 | ✅ |
| R14 | AI 触发后 `payload` 落库 `ai_analysis_result.payload`（JSON 字符串），并生成 `summary` 供列表快速展示 | 单元：`AiAnalysisServiceImplTest.shouldTriggerSummaryAnalysis` / `shouldTriggerRiskAnalysis` | 同名用例 | ✅ |
| R15 | 字典 `(type, key)` 唯一约束 | 单元：`DictServiceImplTest.shouldRejectDuplicateDictKey` | 同名用例 | ✅ |
| R16 | 统一返回体 `Result<T> = {code, message, data, timestamp}`；`code == 0` 为成功 | 单元：`CommonLayerTest.resultSuccessHasCodeZero` / `resultErrorFromErrorCode`；集成：各 `*ControllerIntegrationTest` 断言 `jsonPath("$.code").value(0)` | 同名用例 | ✅ |
| R17 | 业务错误码与 HTTP 状态码语义对齐（409 用于冲突类、404 用于资源不存在、400 用于参数错误、500 用于系统异常） | 单元：`CommonLayerTest.errorCodeConstants` + 各 Service 用例断言 `ErrorCode.BIZ_xxx.getCode()` | 同名用例 | ✅ |
| R18 | 全局异常处理器将 `BizException` 转为 `Result` + 携带 `code` / `message` | 单元：`GlobalExceptionHandlerTest.shouldHandleBizException` / `shouldHandleBizExceptionWithDetail` | 同名用例 | ✅ |
| R19 | `X-User` 头缺省时操作人记为 `anonymous` | 单元：`UserContextTest.shouldReturnAnonymousWhenNotSet` | 同名用例 | ✅ |
| R20 | 状态变更与 `work_item_status_history` 写入**同事务** | 单元：`WorkItemTransitionServiceImplTest.shouldTransitSuccessfully` 用 `Mockito.verify` 验证 `updateById` + `insert` 都被调用；`@Transactional` 注解在 `transit` 方法上 | `shouldTransitSuccessfully` | ✅ |

---

## 3. 状态流转测试

### 3.1 邻接表（实测落地版本）

源自 [StateMachine.java](file:///Users/prafliewen/Desktop/后端方向/sdpm/backend/src/main/java/com/sdpm/workitem/service/StateMachine.java)：

| 源状态 | 允许的目标状态 |
|---|---|
| `DRAFT` | `ANALYZING` |
| `ANALYZING` | `READY`、`DRAFT` |
| `READY` | `IN_PROGRESS`、`ANALYZING` |
| `IN_PROGRESS` | `IN_TESTING`、`READY` |
| `IN_TESTING` | `DONE`、`IN_PROGRESS` |
| `DONE` | （空，终态） |

### 3.2 验证方式

**单元测试**：`StateMachineTest`（12 个用例）

| 分类 | 用例 | 断言 |
|---|---|---|
| 合法流转 | `shouldAllowDraftToAnalyzing` | `canTransit == true`；`assertTransit` 不抛 |
| 合法流转 | `shouldAllowAnalyzingToReady` | `canTransit == true` |
| 合法回退 | `shouldAllowAnalyzingToDraft` | `canTransit == true` |
| 合法流转 | `shouldAllowReadyToInProgress` | `canTransit == true` |
| 合法流转 | `shouldAllowInProgressToInTesting` | `canTransit == true` |
| 合法流转 | `shouldAllowInTestingToDone` | `canTransit == true` |
| 合法回退 | `shouldAllowInTestingToInProgress` | `canTransit == true` |
| 非法跳级 | `shouldRejectDraftToReady` | `assertTransit` 抛 `BIZ_TRANSITION_NOT_ALLOWED` |
| 非法跳级 | `shouldRejectDraftToDone` | `assertTransit` 抛 `BIZ_DONE_IMMUTABLE` |
| 终态不可变 | `shouldRejectDoneToAny` | `canTransit == false`；`assertTransit` 抛 `BIZ_DONE_IMMUTABLE` |
| 非法跨级回退 | `shouldRejectInProgressToDraft` | `canTransit == false` |
| 非法跳级 + DONE | `shouldRejectReadyToDone` | `assertTransit` 抛 `BIZ_DONE_IMMUTABLE` |
| 终态无目标 | `doneShouldHaveNoAllowedTransitions` | `getAllowedTransitions(DONE).isEmpty() == true` |

**Service 集成测试**：`WorkItemTransitionServiceImplTest`

| 分类 | 用例 | 断言 |
|---|---|---|
| 正常流转 | `shouldTransitSuccessfully` | mock `workItemMapper` + `historyMapper`；验证 `stateMachine.assertTransit` 被调用且返回 VO `fromStatus="DRAFT"` / `toStatus="ANALYZING"` |
| 工作项不存在 | `shouldThrowNotFoundOnTransit` | `transit(999L, ...)` 抛 `BIZ_NOT_FOUND` |
| 状态机拦截 | `shouldBeBlockedByStateMachine` | mock `stateMachine.assertTransit` 抛 `BIZ_TRANSITION_NOT_ALLOWED`；验证 `transit` 不再调 `updateById` |
| 守卫链按序执行 | `shouldBeBlockedByGuard` | 验证 `guards` 列表为空时正常通过（兜底用例） |
| 历史查询 | `shouldListHistory` | mock 分页结果 → 断言 VO 字段映射 |
| 空历史 | `shouldReturnEmptyHistory` | mock `records=null` → 返回 `PageResp` 记录数为 0 |

**Controller 集成测试**：`WorkItemTransitionControllerIntegrationTest`

| 分类 | 用例 | 断言 |
|---|---|---|
| 触发流转 | `shouldTransit` | `POST /api/v1/work-items/1/transitions` 200，响应 `data.fromStatus="DRAFT"` / `data.toStatus="ANALYZING"` |
| 缺 `targetStatus` | `shouldRejectTransitWithoutTarget` | 400（JSR-303 校验） |
| 查询历史 | `shouldListHistory` | `GET /api/v1/work-items/1/transitions` 200，断言 `data.total=1` |

### 3.3 手工回归路径

| 场景 | 期望 | 实测 |
|---|---|---|
| 合法流转 DRAFT → ANALYZING → READY → IN_PROGRESS → IN_TESTING → DONE | 全部 200；状态历史同步写入 | ✅ |
| 非法跳级 DRAFT → IN_PROGRESS | 409 `BIZ_TRANSITION_NOT_ALLOWED` | ✅ |
| 任意流转进入或来自 DONE | 409 `BIZ_DONE_IMMUTABLE` | ✅ |
| 跨级回退 `IN_TESTING → DRAFT` | 409 `BIZ_TRANSITION_NOT_ALLOWED` | ✅ |

---

## 4. 澄清问题测试

### 4.1 验证方式

**单元测试**：`P0ClarificationGuardTest`（6 个用例）

| 分类 | 用例 | 断言 |
|---|---|---|
| 存在 P0 OPEN → 进入 READY | `shouldBlockWhenP0OpenExists` | mock `selectCount = 1L`；抛 `BIZ_P0_CLARIFICATION_BLOCKED`；`message` 包含计数 `1` |
| 存在 P0 OPEN → 进入 IN_PROGRESS | `shouldBlockInProgress` | mock `selectCount = 2L`；抛 `BIZ_P0_CLARIFICATION_BLOCKED` |
| 存在 P0 OPEN → 进入 IN_TESTING | `shouldBlockInTesting` | 抛 `BIZ_P0_CLARIFICATION_BLOCKED` |
| 存在 P0 OPEN → 进入 DONE | `shouldBlockDone` | 抛 `BIZ_P0_CLARIFICATION_BLOCKED` |
| P0 OPEN = 0 → 进入 READY | `shouldPassWhenNoP0Open` | mock `selectCount = 0L`；不抛 |
| 非阻断目标（DRAFT） | `shouldPassForNonBlockedStatus` | 不查 DB、不抛 |

> 关键设计：`P0ClarificationGuard` 在每次 `transit` 调用时实时 `selectCount`，不缓存。`ClarificationServiceImpl` 解决澄清问题后**不修改** `work_item.status`——`P0ClarificationGuard` 通过下次流转时查询实时反映新计数（`WorkItemServiceImpl.java:153-155` 详情页 `p0OpenClarifications` 字段当前为 0 是已知 TODO，见 §6）。

**Service 测试**：`ClarificationServiceImplTest`（11 个用例）

| 分类 | 用例 | 断言 |
|---|---|---|
| 新增成功 | `shouldAddQuestion` | 返回 VO `question/severity="P0"/status="OPEN"` |
| 重复 question | `shouldRejectDuplicateQuestion` | 抛 `BIZ_DUPLICATE_QUESTION` |
| 工作项不存在 | `shouldThrowNotFoundOnAddToMissingWorkItem` | 抛 `BIZ_NOT_FOUND` |
| 缺省严重度 | `shouldDefaultToP1Severity` | 入参 `severity=null` → 返回 VO `severity="P1"` |
| 解决成功 | `shouldResolveQuestion` | mock 入库 → 返回 `status="RESOLVED"` + `answer` |
| 解决不存在 | `shouldThrowNotFoundOnResolveMissing` | 抛 `BIZ_NOT_FOUND` |
| 重复解决 | `shouldRejectDoubleResolve` | mock `status="RESOLVED"` → 抛 `BIZ_CLARIFICATION_ALREADY_RESOLVED` |
| 列表查询 | `shouldListQuestions` | mock 分页 → 断言 `total=1` |
| P0 OPEN 计数 | `shouldCountP0Open` | mock `selectCount = 3L` → 返回 3 |
| P0 已解决 | `shouldCountZeroWhenAllResolved` | mock `selectCount = 0L` → 返回 0 |

**Controller 集成测试**：`ClarificationControllerIntegrationTest`（4 个用例）

| 分类 | 用例 | 断言 |
|---|---|---|
| 新增问题 | `shouldAddQuestion` | `POST /api/v1/work-items/1/clarifications` 200 |
| 缺 `question` | `shouldRejectAddWithoutQuestion` | 400 |
| 解决问题 | `shouldResolveQuestion` | `PUT /api/v1/clarifications/1` 200，响应 `data.status="RESOLVED"` |
| 查询列表 | `shouldListQuestions` | `GET /api/v1/work-items/1/clarifications` 200 |

### 4.2 阻断与解除链路（手工回归）

```text
1. 创建工作项（DRAFT）
2. 新增 P0 澄清问题："是否需要支持多语言？"
3. 流转到 READY → 期望 409 BIZ_P0_CLARIFICATION_BLOCKED
4. 解决该澄清问题（填 answer）
5. 再次流转到 READY → 期望 200
```

| 步骤 | 期望 | 实测 |
|---|---|---|
| 新增 P0 后流转 | 409 `BIZ_P0_CLARIFICATION_BLOCKED`，message 包含"1 条未解决的P0" | ✅ |
| 解决 P0 后流转 | 200 | ✅ |
| 解决后再尝试流转到 IN_PROGRESS | 200（守卫实时查询） | ✅ |
| 再次解决同一问题 | 409 `BIZ_CLARIFICATION_ALREADY_RESOLVED` | ✅ |
| 同一工作项下加相同 question | 409 `BIZ_DUPLICATE_QUESTION` | ✅ |

---

## 5. AI 能力测试

### 5.1 验证方式

**Adapter 单元测试**：`MockAiAdapterTest`（6 个用例）

| 分类 | 用例 | 断言 |
|---|---|---|
| SUMMARY | `shouldReturnStructuredSummary` | 返回 Map 含 `headline / background / goal / scope / risks / keyPoints` 6 个 key；`headline` 非空 |
| RISK | `shouldReturnStructuredRisk` | 返回 Map 含 `level / items`；`items` 非 null |
| CLARIFICATION | `shouldReturnStructuredClarification` | 返回 Map 含 `questions`；`questions` 非 null |
| ACCEPTANCE | `shouldReturnStructuredAcceptance` | 返回 Map 含 `criteria / coverage` |
| TASK_BREAKDOWN | `shouldReturnStructuredTaskBreakdown` | 返回 Map 含 `tasks / totalEstimateHours` |
| 主流能力 3 种 | `allMainCapabilitiesReturnNonNull` | SUMMARY / RISK / CLARIFICATION 循环调用，结果非 null 且非空 |

> **关键不变量**：payload 必须是 `Map<String, Object>`（Jackson 可序列化），禁止返回"一段散文"。`MockAiAdapter` 的 `execute` 方法对每次返回做 `deepCopy`（`MockAiAdapter.java:40-47`），保证跨调用不污染。

**Service 单元测试**：`AiAnalysisServiceImplTest`（7 个用例）

| 分类 | 用例 | 断言 |
|---|---|---|
| 触发 SUMMARY | `shouldTriggerSummaryAnalysis` | mock `summaryCapability.analyse` 返回 payload；断言响应 `analysisType="SUMMARY"` / `summary="需求摘要标题"`；`aiAnalysisResultMapper.insert` 被调用 |
| 触发 RISK | `shouldTriggerRiskAnalysis` | 入参 `level="HIGH"` → 响应 `summary="风险等级: HIGH"` |
| 工作项不存在 | `shouldThrowNotFoundOnAnalysis` | 抛 `BIZ_NOT_FOUND` |
| 非法 `analysisType` | `shouldThrowOnInvalidAnalysisType` | 入参 `"INVALID_TYPE"` → 抛 `BIZ_AI_CAPABILITY_NOT_FOUND` |
| 空 payload | `shouldThrowOnEmptyResult` | mock `analyse` 返回 `emptyMap` → 抛 `BIZ_AI_SCHEMA_INVALID` |
| 历史查询 | `shouldListAnalyses` | mock 分页 → 断言 `total=1` / `analysisType="SUMMARY"` |
| 空历史 | `shouldReturnEmptyAnalysisList` | mock `records=[]` → 返回 `total=0` |

**Controller 集成测试**：`AiAnalysisControllerIntegrationTest`（3 个用例）

| 分类 | 用例 | 断言 |
|---|---|---|
| 触发分析 | `shouldTriggerAnalysis` | `POST /api/v1/work-items/1/ai-analyses` 200，响应 `data.analysisType="SUMMARY"` / `data.source="MOCK"` |
| 缺 `analysisType` | `shouldRejectTriggerWithoutType` | 400 |
| 查询历史 | `shouldListAnalyses` | `GET /api/v1/work-items/1/ai-analyses` 200 |

### 5.2 能力注册矩阵（实测落地）

| `analysisType` | `AiAnalysisTypeEnum` | `MockAiAdapter` payload | `AiCapability` 实现类 | 路由可触发 |
|---|---|---|---|---|
| `SUMMARY` | ✅ | ✅ 6 字段 | `SummaryCapability` | ✅ |
| `RISK` | ✅ | ✅ `level + items` | `RiskCapability` | ✅ |
| `CLARIFICATION` | ✅ | ✅ `questions` | `ClarificationCapability` | ✅ |
| `ACCEPTANCE` | ✅ | ✅ `criteria + coverage` | ❌ 未实现（详见 §6） | 触发时抛 `BIZ_AI_CAPABILITY_NOT_FOUND` |
| `TASK_BREAKDOWN` | ✅ | ✅ `tasks + totalEstimateHours` | ❌ 未实现（详见 §6） | 触发时抛 `BIZ_AI_CAPABILITY_NOT_FOUND` |

### 5.3 路由与持久化路径（手工回归）

```text
1. 创建工作项（DRAFT 或任意状态）
2. 触发 SUMMARY → 期望 200，data.payload.headline 非空，data.summary=headline
3. 触发 RISK → 期望 200，data.summary="风险等级: MEDIUM"
4. 触发 ACCEPTANCE → 期望 409 BIZ_AI_CAPABILITY_NOT_FOUND（Capability 未注册）
5. 查询历史 → 期望 200，data.records 含 2 条（MOCK 源）
```

| 步骤 | 期望 | 实测 |
|---|---|---|
| 触发 SUMMARY | 200，summary 非空 | ✅ |
| 触发 RISK | 200，summary 形如"风险等级: XXX" | ✅ |
| 触发 ACCEPTANCE | 409 `BIZ_AI_CAPABILITY_NOT_FOUND` | ✅ |
| 同 key 5min 内重复触发（幂等） | 当前**未实现**（详见 §6） | ⚠️ |

### 5.4 持久化与可追溯

- 每次触发都写一行 `ai_analysis_result`（`source="MOCK"`，`payload` 为 JSON 字符串）。
- `GET /api/v1/work-items/{id}/ai-analyses?analysisType=SUMMARY&pageNo=1&pageSize=10` 支持按类型分页查询历史。
- 详情页 AI 分析 Tab 渲染 `payload` 树 + `summary` 顶部摘要（前端 `WorkItemDetail.vue` 内部 `JSON.stringify(payload, null, 2)`）。

---

## 6. 未覆盖风险

> 主动识别 + 显式声明；为评审、二次接手提供完整风险地图。

### 6.1 测试覆盖空白

| # | 风险 | 影响 | 当前缓解 | 后续成本 |
|---|---|---|---|---|
| W1 | **真实数据库集成**未做（仅 Mockito 桩 Mapper） | 集成 SQL / 索引 / 事务回滚 / `@TableLogic` 行为只能靠手工回归 | `application-test.yml` 启动 Spring 上下文 + H2 内存库（Controller 集成测试覆盖 Bean 装配） | 中：接入 Testcontainers + MySQL 8 容器 |
| W2 | **并发 / 竞态**未做（无 `@RepeatedTest` / 多线程用例） | 乐观锁、状态机并发流转、幂等键冲突无法验证 | `WorkItemServiceImplTest.shouldThrowVersionConflict` 单线程验证 | 中：JMH / 多线程用例 |
| W3 | **接口幂等**（`Idempotency-Key`）未在网关层强制 | `api-design.md §7` 已设计，未实现 | 写接口按 `id` 天然幂等 | 中 |
| W4 | **`AcceptanceCapability` / `TaskBreakdownCapability`** 仅枚举与 Mock payload 就绪，**未注册 `AiCapability` 实现类** | 触发 `ACCEPTANCE` / `TASK_BREAKDOWN` 时抛 `BIZ_AI_CAPABILITY_NOT_FOUND` | `MockAiAdapterTest` 覆盖 5 种 payload 模板；新增 `*Capability` 5 分钟可补 | 低 |
| W5 | **真实 LLM 适配器**（`LlmAiAdapter`）未实现 | AI 能力仅 Mock，无法对接 OpenAI / Claude | `AiAdapter` 接口已留位；`MockAiAdapter` 可作为基线对比 | 中：prompt 工程 + 重试 + 超时 |
| W6 | **`X-Role: admin` 强校验**未在 `DictController` 写接口执行 | 管理接口当前允许任意 `X-User` 调用 | `api-design.md §4.3` 已设计 | 低 |
| W7 | **详情页 `p0OpenClarifications` / `totalOpenClarifications`** 当前硬编码为 0 | 前端按钮禁用逻辑暂时失效 | `WorkItemServiceImpl.java:153-155` 已留 TODO | 极低：注入 `ClarificationService.countP0Open(id)` |
| W8 | **跨态回退 `IN_PROGRESS → DRAFT`** 用例已覆盖非法，但 **"需求变更导致的回退"** 业务路径未抽象为守卫 | 当前只能回退到相邻状态 | 邻接表清晰、可读 | 低 |
| W9 | **MyBatis-Plus `@TableLogic` 软删除 + 状态机 DONE 不可删**两条规则串联时，若 `deleted=1` 的工作项被查回，`status="DONE"` 检查会失效 | DONE 软删检查依赖 `@TableLogic` 自动过滤；当前实测 OK | 单元测试 `shouldRejectDeleteDone` | 低 |
| W10 | **前端 E2E（Playwright / Cypress）**未做 | 列表 / 详情 / 弹窗之间的串联交互只能手工验证 | Vitest 覆盖了关键模块与视图 | 中 |

### 6.2 已知技术债务

| # | 债务 | 触发原因 | 缓解方式 |
|---|---|---|---|
| T1 | `mockResponses` 在 `MockAiAdapter` 构造器中初始化，`@Component` 单例 → 多线程并发调用安全（payload `deepCopy`），但若未来 `MockAiAdapter` 改为有状态需同步 | 当前实现已 `deepCopy` | 显式补 `@ThreadSafe` 注释或文档化 |
| T2 | `AiAnalysisServiceImpl` 在 `payload` 序列化失败时抛 `BIZ_AI_SCHEMA_INVALID` 而非降级 | schema 强校验是设计原则 | 不动 |
| T3 | `WorkItemController` 的 `DELETE /api/v1/work-items/{id}` 未走幂等键 | 软删除天然幂等 | 不动 |
| T4 | `GlobalExceptionHandler.handleException` 对未知异常统一返回 `SYS_INTERNAL` + 通用 message | 避免泄露堆栈 | 配 `logback-spring.xml` 把堆栈写 ERROR |
| T5 | `UserContext` 是 ThreadLocal，未提供 `TenantContext` | 单租户场景 | 多租户时加 `TenantContextFilter` |
| T6 | `tags` / `acceptanceCriteria` / `payload` 用 JSON 列存；MySQL 8 才有 JSON 函数 | 团队规范默认 MySQL 8 | 不动 |

### 6.3 演示中可能踩的坑

| 场景 | 现象 | 解决 |
|---|---|---|
| `mvn test` 跑不通 | 端口占用 / 缺 MySQL JDBC 驱动 | 集成测试使用 H2，**不需要 MySQL**；运行 `mvn test` 即可 |
| 启动 `application-dev.yml` 报数据源连接失败 | 本地无 MySQL | 切换到 `application-demo.yml`（已配 H2）或 `application-test.yml` |
| 前端 `npm run dev` 报 404 | 端口被占用或代理未生效 | `vite.config.js` 已配 `/api → http://localhost:8080` |
| 触发 `ACCEPTANCE` AI 报 409 | Capability 未注册 | 当前预期行为；详见 §6.1 W4 |
| 工作项详情 P0 计数显示 0 | 已知 TODO（§6.1 W7） | 不影响流转守卫（守卫每次实时查 DB） |

---

## 7. 测试执行

### 7.1 后端

```bash
cd backend
mvn test
```

- 期望输出：`Tests run: 120+, Failures: 0, Errors: 0, Skipped: 0`。
- 报告位置：`backend/target/surefire-reports/TEST-*.xml` + `*.txt`。
- 测试 profile：`backend/src/test/resources/application-test.yml`（H2 内存库）。

### 7.2 前端

```bash
cd frontend
npm install
npm test
```

- 期望输出：所有 Vitest 用例通过。
- 覆盖率（如开启）：`npx vitest --coverage`。

### 7.3 端到端手工回归

完整脚本见 [../process.md §7.3](../process.md#73-端到端演示脚本)。

### 7.4 契约可视化

- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

---

## 8. 验收对照（与 [02_提交规范.md §6](../02_提交规范.md) 5 条要求的对应关系）

| 提交规范要求 | 本文档章节 | 状态 |
|---|---|---|
| 1. 测试了哪些核心业务规则 | §2 核心业务规则验证（R1~R20） | ✅ |
| 2. 如何验证状态流转 | §3 状态流转测试（邻接表 + 12 单元用例 + 6 Service 用例 + 3 Controller 用例 + 手工回归） | ✅ |
| 3. 如何验证澄清问题阻断规则 | §4 澄清问题测试（6 守卫用例 + 11 Service 用例 + 4 Controller 用例 + 手工回归） | ✅ |
| 4. 如何验证 AI 辅助能力 | §5 AI 能力测试（6 Adapter 用例 + 7 Service 用例 + 3 Controller 用例 + 能力注册矩阵 + 手工回归） | ✅ |
| 5. 有哪些未覆盖风险 | §6 未覆盖风险（10 项覆盖空白 + 6 项技术债务 + 5 项演示踩坑） | ✅ |

---

## 附录 A：测试类文件清单

```text
backend/src/test/java/com/sdpm/workitem/
├── AiAnalysisControllerIntegrationTest.java
├── AiAnalysisServiceImplTest.java
├── ClarificationControllerIntegrationTest.java
├── ClarificationServiceImplTest.java
├── CommonLayerTest.java
├── DictControllerIntegrationTest.java
├── DictServiceImplTest.java
├── GlobalExceptionHandlerTest.java
├── MockAiAdapterTest.java
├── P0ClarificationGuardTest.java
├── StateMachineTest.java
├── UserContextTest.java
├── WorkItemControllerIntegrationTest.java
├── WorkItemServiceImplTest.java
├── WorkItemTransitionControllerIntegrationTest.java
└── WorkItemTransitionServiceImplTest.java
```

## 附录 B：关键断言速查

| 断言 | 含义 |
|---|---|
| `canTransit(from, to) == true` | 邻接表内流转 |
| `assertTransit` 不抛 | 邻接表 + DONE 不可变校验通过 |
| `BIZ_TRANSITION_NOT_ALLOWED` | 邻接表外 / 跨级回退 |
| `BIZ_DONE_IMMUTABLE` | 源或目标为 DONE |
| `BIZ_P0_CLARIFICATION_BLOCKED` | P0 OPEN > 0 且目标 ∈ {READY, IN_PROGRESS, IN_TESTING, DONE} |
| `BIZ_DUPLICATE_QUESTION` | 同工作项下重复 question |
| `BIZ_CLARIFICATION_ALREADY_RESOLVED` | 二次解决 |
| `BIZ_VERSION_CONFLICT` | 乐观锁错配 |
| `BIZ_AI_CAPABILITY_NOT_FOUND` | `analysisType` 非法或 Capability 未注册 |
| `BIZ_AI_SCHEMA_INVALID` | payload 缺失关键 key 或序列化失败 |
| `code == 0` | 业务成功 |
| `code != 0` | 业务失败（统一 `Result` 包装） |
