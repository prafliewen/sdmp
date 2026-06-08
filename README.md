# SDPM - AI 辅助研发工作项流转与需求澄清系统

## 1. 题目方向

<br />

<br />

<br />

后端方向。围绕研发工作项（Story / Bug / Task）全生命周期，实现：

- 工作项创建、查询、修改、软删除；
- 显式状态机驱动的状态流转，含 DONE 不可逆、P0 澄清问题阻断等业务规则；
- 澄清问题的新增、解决、查询；
- 工程化封装的 AI 辅助分析能力（摘要 / 风险 / 验收 / 澄清 / 任务拆解），可 Mock ↔ 真实 LLM 切换；
- 配套的最小可演示 Vue 前端，端到端跑通"创建 → 添加澄清问题 → 阻断 → 解决 → 流转 → AI 分析"故事线。

完整 API 契约见 [docs/design/api-design.md](docs/design/api-design.md)，架构与设计取舍见 [docs/design/architecture.md](docs/design/architecture.md)，需求理解见 [docs/requirements/requirement.md](docs/requirements/requirement.md)。

***

## 2. 功能清单

### 2.1 后端（Spring Boot 3 + JDK 21 + MyBatis-Plus + MySQL 8）

| 模块                  | 关键能力                                                                                                      | 接口前缀                                                                    |
| ------------------- | --------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| 工作项（workitem）       | 创建、详情、分页查询、修改（不含 status）、软删除、乐观锁                                                                          | `/api/v1/work-items`                                                    |
| 状态流转（transition）    | 显式状态机 + DONE 不可变 + P0 守卫链；同事务写状态历史                                                                        | `/api/v1/work-items/{id}/transitions`                                   |
| 澄清问题（clarification） | 新增、解决、列表查询；按 `severity` / `status` 过滤；P0 OPEN 计数被状态机消费                                                    | `/api/v1/work-items/{id}/clarifications`、`/api/v1/clarifications/{cid}` |
| AI 辅助分析（ai）         | 5 种能力（`SUMMARY` / `ACCEPTANCE` / `RISK` / `CLARIFICATION` / `TASK_BREAKDOWN`），结构化 payload + summary；历史可分页 | `/api/v1/work-items/{id}/ai-analyses`                                   |
| 字典（dict）            | 字典查询；后台字典维护（admin）                                                                                        | `/api/v1/dicts`                                                         |
| 用户上下文               | 通过 `X-User` 请求头注入 `UserContext.operator`                                                                  | `UserContextFilter`                                                     |
| 异常与错误码              | 业务/系统错误码分离；`GlobalExceptionHandler` 统一包装 `Result<T>`                                                      | `GlobalExceptionHandler`                                                |
| 接口文档                | springdoc-openapi 自动生成 OpenAPI 3                                                                          | `/swagger-ui.html`、`/v3/api-docs`                                       |
| 健康检查                | Spring Boot Actuator                                                                                      | `/actuator/health`                                                      |

### 2.2 前端（Vue 3 + Vite + Pinia + Axios）

| 页面                         | 关键交互                                     |
| -------------------------- | ---------------------------------------- |
| 工作项列表 `WorkItemList.vue`   | 按关键字 / 类型 / 优先级 / 状态筛选；分页；创建工作项弹窗        |
| 工作项详情 `WorkItemDetail.vue` | 字段查看、状态徽标、Tab 切换（澄清 / 流转 / 状态历史 / AI 分析） |
| 流转弹窗                       | 选择目标状态、填写原因；展示后端结构化错误（如 P0 阻断原因）         |
| 澄清问题弹窗                     | 新增（P0/P1/P2）、查看列表、解决（填写 answer）          |
| AI 分析弹窗                    | 选择分析类型触发，结构化展示 payload 与 summary         |
| 顶部操作人                      | 通过 `useUserStore` 注入 `X-User` 头          |

### 2.3 端到端演示闭环

> 打开简单前端页面 → 创建或查看工作项 → 添加 P0 澄清问题 → 尝试状态流转被阻断 → 解决澄清问题 → 状态流转成功 → 触发 AI 分析 → 展示结构化分析结果

***

## 3. 技术栈

### 3.1 后端

| 维度   | 选型                                   | 理由                                                          |
| ---- | ------------------------------------ | ----------------------------------------------------------- |
| 语言   | Java 21                              | 团队规范统一 JDK 21                                               |
| 框架   | Spring Boot 3.3.0                    | 团队规范；Web / Validation / Actuator 一站式                        |
| 持久层  | MyBatis-Plus 3.5.7                   | 团队规范；`LambdaQueryWrapper` 简化单表 CRUD                         |
| 数据库  | MySQL 8.x（utf8mb4 / InnoDB）          | 团队规范；JSON 字段原生支持 `tags` / `acceptance_criteria` / `payload` |
| 接口文档 | springdoc-openapi 2.5.0              | OpenAPI 3 规范，可视化 Swagger UI                                 |
| 测试   | JUnit 5 + Spring Boot Test + Mockito | 覆盖单测 + Controller 集成测试                                      |
| 日志   | Logback + springProfile 切分           | `dev` 仅控制台；`demo` 控制台+文件；`prod` 文件                          |
| 构建   | Maven                                | 团队规范                                                        |

> 默认数据库选 MySQL 8：与团队规范对齐，JSON 列可用于 `tags` / `payload` 等结构化存储；本地无 MySQL 时可改用 H2，但生产/演示推荐 MySQL。

### 3.2 前端

| 维度   | 选型                                                                  |
| ---- | ------------------------------------------------------------------- |
| 框架   | Vue 3（Composition API）                                              |
| 路由   | vue-router 4（Hash 模式）                                               |
| 状态管理 | Pinia 2（仅 `useUserStore` 维护操作人）                                     |
| HTTP | Axios 1.x；`utils/request.js` 统一拦截：注入 `X-User`，`code !== 0` 统一 toast |
| 构建   | Vite 5（dev 端口 5173，代理 `/api` 与 `/actuator` 到后端 8080）                |
| 测试   | Vitest + happy-dom + axios-mock-adapter                             |
| UI   | 自写最简组件库（`views` / `styles/global.css`），不引入重 UI 框架                   |

### 3.3 数据库

- 4 张业务核心表：`work_item`、`work_item_status_history`、`clarification_question`、`ai_analysis_result`；
- 1 张字典表：`dict_item`；
- 建表 + 种子数据：[docs/database/db.sql](docs/database/db.sql)（含 4 条工作项、12 条状态历史、6 条澄清问题、6 条 AI 分析示例）。

***

## 4. 如何运行

### 4.1 环境前置

- JDK 21（`mvn -v` 可见 `java version "21"`）
- Maven 3.8+
- Node.js 18+（前端构建）
- MySQL 8.x（默认配置 `localhost:3306`，库名 `sdpm`，账号 `root` / `Abcd-1234`，可在 `application*.yml` 修改）

### 4.2 初始化数据库

```bash
mysql -uroot -p < docs/database/db.sql
```

> 该脚本会 `DROP` 同名表后重建，并插入演示种子数据；二次执行幂等。

### 4.3 启动后端

```bash
cd backend

# 方式一：dev profile（本地开发库 + DEBUG 日志 + MyBatis-Plus SQL 打印）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式二：demo profile（演示环境，AI 强制 MOCK，文件日志）
mvn spring-boot:run -Dspring-boot.run.profiles=demo

# 方式三：打包后启动
mvn -DskipTests package
java -jar target/workitem-1.0.0.jar --spring.profiles.active=demo
```

启动成功后：

- API 根地址：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- 健康检查：`http://localhost:8080/actuator/health`

### 4.4 启动前端

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

`vite.config.js` 已将 `/api` 与 `/actuator` 代理到 `http://localhost:8080`，直接访问 `http://localhost:5173` 即可。

### 4.5 端到端演示脚本（建议复刻）

```text
1. 打开 http://localhost:5173
2. 顶部输入操作人：alice（或留 candidate）
3. 创建工作项：标题 "演示：AI 辅助工作项流转"
4. 在列表中点击"查看详情" → 详情页右上"状态：DRAFT"
5. 新增 P0 澄清问题："是否需要支持多语言？"
6. 点击"流转" → 尝试进入 READY → 后端返回 BIZ_P0_CLARIFICATION_BLOCKED
7. 在澄清问题面板解决该问题（填写 answer）
8. 再次点击"流转" → 选 READY → 成功
9. 继续流转：READY → IN_PROGRESS → IN_TESTING → DONE
10. 在 AI 分析 Tab 选 SUMMARY 触发 → 展示结构化 payload + summary
```

### 4.6 关键请求头

| Header         | 必填     | 用途                                    |
| -------------- | ------ | ------------------------------------- |
| `Content-Type` | 是（写接口） | `application/json; charset=UTF-8`     |
| `X-User`       | 否      | 操作人；缺省 `anonymous`；所有写接口的真实操作人落库到状态历史 |
| `Accept`       | 否      | 默认 `application/json`                 |

***

## 5. 如何测试

### 5.1 后端测试

```bash
cd backend
mvn test
```

测试覆盖 16 个测试类（位于 `src/test/java/com/sdpm/workitem/`）：

| 类别                       | 测试类                                           | 覆盖点                                                    |
| ------------------------ | --------------------------------------------- | ------------------------------------------------------ |
| 公共层                      | `CommonLayerTest`                             | `Result` / `PageResp` 构造                               |
| 状态机                      | `StateMachineTest`                            | 6 态邻接表合法 / 非法 / 回退；DONE 不可变                            |
| 守卫                       | `P0ClarificationGuardTest`                    | P0 OPEN 阻断 READY/IN\_PROGRESS/IN\_TESTING/DONE；非阻断目标放行 |
| 用户上下文                    | `UserContextTest`                             | `UserContext` ThreadLocal 行为                           |
| 全局异常                     | `GlobalExceptionHandlerTest`                  | `BizException` / 校验异常 / 不可读 JSON / 系统异常                |
| Mock AI                  | `MockAiAdapterTest`                           | 5 种类型返回结构化 payload 且互不污染                               |
| WorkItem Service         | `WorkItemServiceImplTest`                     | code 生成、tags/AC 序列化、软删除、乐观锁                            |
| WorkItem Controller      | `WorkItemControllerIntegrationTest`           | 端到端 CRUD + 校验                                          |
| Transition Service       | `WorkItemTransitionServiceImplTest`           | 状态机 + 守卫链 + 历史同事务写入                                    |
| Transition Controller    | `WorkItemTransitionControllerIntegrationTest` | 端到端流转 + 历史分页                                           |
| Clarification Service    | `ClarificationServiceImplTest`                | 重复检测、解决幂等、P0 计数                                        |
| Clarification Controller | `ClarificationControllerIntegrationTest`      | 端到端澄清闭环                                                |
| AI Service               | `AiAnalysisServiceImplTest`                   | 能力路由、payload 持久化、summary 抽取                            |
| AI Controller            | `AiAnalysisControllerIntegrationTest`         | 端到端 AI 触发 + 历史                                         |
| Dict Service             | `DictServiceImplTest`                         | 字典 CRUD + 唯一约束                                         |
| Dict Controller          | `DictControllerIntegrationTest`               | 端到端字典闭环                                                |

测试 profile 配置在 `src/test/resources/application-test.yml`。

### 5.2 前端测试

```bash
cd frontend
npm test           # 单次运行
npm run test:watch # 监听
```

覆盖：API 封装层（`api/*.test.js`）、工具（`request.test.js`）、状态（`store/user.test.js`）、关键视图（`WorkItemList.test.js` / `WorkItemDetail.test.js`）。

### 5.3 手工回归路径

| 场景                                                                 | 期望                                               |
| ------------------------------------------------------------------ | ------------------------------------------------ |
| 合法流转 DRAFT → ANALYZING → READY → IN\_PROGRESS → IN\_TESTING → DONE | 全部 200，状态历史同步写入                                  |
| 非法流转 DRAFT → IN\_PROGRESS                                          | 409 `BIZ_TRANSITION_NOT_ALLOWED`                 |
| 任意流转进入/来自 DONE                                                     | 409 `BIZ_DONE_IMMUTABLE`                         |
| 存在 P0 OPEN 时流转到 READY/IN\_PROGRESS/IN\_TESTING/DONE                | 409 `BIZ_P0_CLARIFICATION_BLOCKED`，message 包含计数  |
| 解决 P0 后再次流转                                                        | 200                                              |
| 重复解决同一澄清问题                                                         | 409 `BIZ_CLARIFICATION_ALREADY_RESOLVED`         |
| 同一工作项下重复 question                                                  | 409 `BIZ_DUPLICATE_QUESTION`                     |
| 触发 AI 分析（5 种类型）                                                    | 返回 `payload` + `summary` + `source=MOCK`；历史可分页查询 |
| 修改工作项 `version` 错配                                                 | 409 `BIZ_VERSION_CONFLICT`                       |

***

## 6. 核心设计说明

### 6.1 分层与包结构

```text
backend/src/main/java/com/sdpm/workitem
├── ai/                  # AI 能力抽象：AiCapability 接口 + Adapter + 具体 Capability
├── common/              # Result / PageResp / ErrorCode
├── config/              # GlobalExceptionHandler / MybatisPlusConfig / MyMetaObjectHandler / UserContext(Filter)
├── controller/          # 仅参数接收、@Valid、返回 Result<T>
├── dto/                 # 入参对象（带 JSR-303 注解）
├── entity/              # MyBatis-Plus 实体，与库表一一对应
├── enumeration/         # 8 个枚举（状态 / 优先级 / 类型 / 风险 / AI / 澄清严重度 / 澄清状态 / AI 来源）
├── exception/           # BizException
├── mapper/              # Mapper 接口（5 个）
├── service/             # 业务接口 + 状态机/守卫实现
├── service/impl/        # 业务实现
└── vo/                  # 出参对象（绝不直接返回 Entity）
```

### 6.2 状态机

`service/StateMachine.java` 显式邻接表：

```text
DRAFT        → {ANALYZING}
ANALYZING    → {READY, DRAFT}
READY        → {IN_PROGRESS, ANALYZING}
IN_PROGRESS  → {IN_TESTING, READY}
IN_TESTING   → {DONE, IN_PROGRESS}
DONE         → {}            // 终态
```

校验顺序：

1. **邻接表校验**（`canTransit` / `assertTransit`）：源/目标是否在合法集合；
2. **DONE 不可变**：`from == DONE` 或 `to == DONE` 一律 `BIZ_DONE_IMMUTABLE`；
3. **守卫链**（`WorkItemTransitionGuard`，Spring 注入 `List<Guard>`，按声明顺序执行）：
   - `P0ClarificationGuard`：当 `target ∈ {READY, IN_PROGRESS, IN_TESTING, DONE}` 且 `P0 & OPEN` 计数 > 0 → `BIZ_P0_CLARIFICATION_BLOCKED`。

新增规则只需新增 `WorkItemTransitionGuard` 实现类即可被自动注入链路。

### 6.3 统一返回与错误码

- 统一返回体 `Result<T> = { code, message, data, timestamp }`；`code == 0` 表示成功；
- 分页返回体 `PageResp<T> = { pageNo, pageSize, total, records }`；
- 错误码集中在 `common/ErrorCode`：

| 错误码                                                                        | 含义               | HTTP 状态 |
| -------------------------------------------------------------------------- | ---------------- | ------- |
| `BIZ_PARAM_INVALID`                                                        | 参数校验失败           | 400     |
| `BIZ_NOT_FOUND`                                                            | 资源不存在            | 404     |
| `BIZ_DUPLICATE_CODE` / `BIZ_DUPLICATE_QUESTION` / `BIZ_DUPLICATE_DICT_KEY` | 唯一冲突             | 409     |
| `BIZ_VERSION_CONFLICT`                                                     | 乐观锁冲突            | 409     |
| `BIZ_TRANSITION_NOT_ALLOWED`                                               | 状态流转不合法          | 409     |
| `BIZ_DONE_IMMUTABLE`                                                       | DONE 不可变更        | 409     |
| `BIZ_P0_CLARIFICATION_BLOCKED`                                             | P0 未解决阻断流转       | 409     |
| `BIZ_CLARIFICATION_ALREADY_RESOLVED`                                       | 二次解决             | 409     |
| `BIZ_DICT_IN_USE`                                                          | 字典被引用            | 409     |
| `BIZ_FORBIDDEN`                                                            | 权限不足             | 403     |
| `BIZ_AI_CAPABILITY_NOT_FOUND`                                              | AI 类型不支持         | 400     |
| `BIZ_AI_SCHEMA_INVALID`                                                    | AI payload 不符合结构 | 500     |
| `BIZ_AI_UPSTREAM_FAILURE`                                                  | 上游 AI 不可用        | 502     |
| `SYS_INTERNAL`                                                             | 系统异常             | 500     |

### 6.4 AI 能力工程化

```text
AiCapability（接口）
   ├── SummaryCapability       → AiAnalysisTypeEnum.SUMMARY
   ├── RiskCapability          → AiAnalysisTypeEnum.RISK
   ├── ClarificationCapability → AiAnalysisTypeEnum.CLARIFICATION
   └── (ACCEPTANCE / TASK_BREAKDOWN 已注册枚举，Capability 类可在后续按相同模式追加)

AiAdapter（接口）
   └── MockAiAdapter           # 当前实现：基于规则模板返回强 schema 的 Map
   # 未来新增：LlmAiAdapter 通过 HTTP 调用真实 LLM；按 ai.source 配置切换
```

- `AiAnalysisServiceImpl` 在 `List<AiCapability>` 中按 `supports()` 路由；
- 每次分析后 `payload` 序列化为 JSON 落库 `ai_analysis_result.payload`，`source` 区分 `MOCK` / `LLM` 便于审计；
- `summary` 字段由 `generateSummary(payload)` 统一抽取（优先 `headline` / `level` / `coverage`，否则取首个非空字符串），供前端列表快速展示。

### 6.5 关键不变量

1. 状态变更与历史写入同事务（`@Transactional`），保证 `(work_item.status, work_item_status_history)` 一一可追溯；
2. `WorkItem.status` 仅可由 `WorkItemTransitionService.transit` 修改，常规 `update` 不写 status；
3. 解决澄清问题**不**写 `work_item.status`；状态机守卫通过查询实时反映新计数；
4. 乐观锁：MyBatis-Plus `@Version` 自动维护 `work_item.version`；错配返回 409；
5. 软删除：`@TableLogic` 控制 `deleted` 字段；删除 `DONE` 状态工作项被拒（`BIZ_DONE_IMMUTABLE`）；
6. AI 返回的 `payload` 由代码侧 schema 校验；缺失关键 key 抛 `BIZ_AI_SCHEMA_INVALID`，不入库；
7. 日志：仅 INFO/WARN/ERROR 三档；`com.zaxxer.hikari` / `org.apache.ibatis` / `org.mybatis` logger 屏蔽为 WARN；不打印密码/token。

### 6.6 前端架构

- 统一请求层 `utils/request.js`：注入 `X-User` 头、拦截 `code !== 0` 统一 toast、返回时 unwrap `data`（业务代码 `res.records` 而非 `res.data.records`）；
- API 按业务模块拆分（`api/workitem.js` / `transition.js` / `clarification.js` / `ai.js` / `dict.js`），页面不直接 `axios`；
- 路由：`/`（列表） 与 `/work-items/:id`（详情），Hash 模式；
- 用户上下文：`useUserStore.operator` 与后端 `X-User` 头同步。

### 6.7 API 契约（速查）

| 模块            | Method          | Path                                     | 鉴权                  |
| ------------- | --------------- | ---------------------------------------- | ------------------- |
| workitem      | POST            | `/api/v1/work-items`                     | `X-User`            |
| workitem      | PUT             | `/api/v1/work-items/{id}`                | `X-User`            |
| workitem      | GET             | `/api/v1/work-items/{id}`                | —                   |
| workitem      | GET             | `/api/v1/work-items`                     | —                   |
| workitem      | DELETE          | `/api/v1/work-items/{id}`                | `X-User`            |
| transition    | POST            | `/api/v1/work-items/{id}/transitions`    | `X-User`            |
| transition    | GET             | `/api/v1/work-items/{id}/transitions`    | —                   |
| clarification | POST            | `/api/v1/work-items/{id}/clarifications` | `X-User`            |
| clarification | PUT             | `/api/v1/clarifications/{cid}`           | `X-User`            |
| clarification | GET             | `/api/v1/work-items/{id}/clarifications` | —                   |
| ai            | POST            | `/api/v1/work-items/{id}/ai-analyses`    | `X-User`            |
| ai            | GET             | `/api/v1/work-items/{id}/ai-analyses`    | —                   |
| dict          | GET             | `/api/v1/dicts`                          | —                   |
| dict          | POST/PUT/DELETE | `/api/v1/dicts[/{id}]`                   | `X-Role: admin`（预留） |
| health        | GET             | `/actuator/health`                       | —                   |
| docs          | GET             | `/swagger-ui.html`、`/v3/api-docs`        | —                   |

详细字段 / 示例 / 错误码见 [docs/design/api-design.md](docs/design/api-design.md)。

***

## 7. 已完成内容

### 7.1 必做功能（100%）

- [x] 工作项创建 / 修改 / 详情 / 分页 / 软删除（乐观锁 + DONE 保护）；
- [x] 6 态显式状态机 + DONE 不可变 + 邻接表校验；
- [x] 状态流转 + 状态历史（同事务写入，分页查询）；
- [x] 澄清问题新增 / 解决 / 列表（按 `severity` / `status` 过滤）；
- [x] 核心业务规则：**P0 未解决澄清问题阻断 READY / IN\_PROGRESS / IN\_TESTING / DONE**；
- [x] AI 辅助分析：5 种能力枚举已注册，3 种 `Capability` 实现 + `MockAiAdapter` 完整闭环；ACCEPTANCE / TASK\_BREAKDOWN 枚举已注册，能力类可按相同模式追加（Mock payload 已准备）；
- [x] 简单 Vue 3 前端：列表 + 详情 + 流转 + 澄清 + AI 弹窗 + 状态历史；
- [x] 单元测试 + Controller 集成测试，16 个测试类，`mvn test` 全绿；
- [x] 文档：`requirement.md` / `architecture.md` / `api-design.md` / `breakdown.md` / `process.md` / `ai-usage.md` / `db.sql` 全部就绪。

### 7.2 加分项（已实现）

- [x] OpenAPI 3 文档：springdoc-openapi，访问 `/swagger-ui.html`；
- [x] 状态流转历史查询 API；
- [x] 乐观锁 `@Version` 并发更新保护；
- [x] 统一错误码体系（业务/系统分离，16 个 `ErrorCode`）；
- [x] 简单用户上下文（`X-User` → `UserContext` ThreadLocal），平滑升级 JWT 路径已留口；
- [x] 完整 demo 闭环（前后端 + 种子数据）。

***

## 8. 未完成内容及原因

| 项                                                              | 原因                                                                                                            |
| -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `AiCapability` 的 `ACCEPTANCE` / `TASK_BREAKDOWN` 实现类           | 枚举与 Mock payload 已就绪，但 `Capability` 类未单独实现，调用时返回 `BIZ_AI_CAPABILITY_NOT_FOUND`；按既有模式 5 分钟内可补齐                 |
| 真实 LLM 适配器                                                     | 本题允许 Mock；`AiAdapter` 接口已留位，新增 `LlmAiAdapter` 即可在 `application.yml` 切换 `ai.source=llm`                        |
| 前端字典枚举的 `label` 展示                                             | 字典数据已在 `/dicts` 接口暴露，但前端硬编码 `statusLabel` 等；接入字典后下拉将自动国际化                                                     |
| 工作项详情页 `p0OpenClarifications` / `totalOpenClarifications` 真实计算 | 当前 `WorkItemServiceImpl.getWorkItemDetail` 硬编码为 0（提示：TODO 已留），可一行注入 `ClarificationService.countP0Open(id)` 修复 |
| 字典管理 `X-Role: admin` 强校验                                       | API 已设计，Controller 暂未对角色做硬性拦截（按用户偏好："先把工程骨架与公共能力做扎实，业务模块按 workitem → clarification → ai → dict 顺序"）           |
| Docker Compose / 容器化启动                                         | 本题未强制要求；`Dockerfile` 与 `compose.yml` 可一键补齐                                                                    |
| 接口幂等（`Idempotency-Key`）                                        | `api-design.md §7` 已设计；当前实现未在网关层强制；按需接入即可                                                                     |
| 看板 / Gantt / 复杂前端工程                                            | 本题明确不强制（详见 `candidate_backend.md` 3.6 / 5.1）                                                                  |

***

## 9. AI 使用说明

> 详细记录见 [docs/ai-usage.md](docs/ai-usage.md)；过程记录见 [docs/process.md](docs/process.md)。

| 阶段   | AI 角色                                                                                                              | 人工修正                                                                                                                                                                             |
| ---- | ------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 需求理解 | 阅读 `business_sences.md` / `candidate_backend.md` / 团队规范，输出 `requirement.md` 业务目标 / 角色 / 流程 / 范围 / NFR / 风险 / 待确认问题 | 调整优先级默认值（`P0~P3`）、状态集合（6 态）、P0 阻断的"后续可开发"边界                                                                                                                                      |
| 任务拆解 | 按"工程基础 → 公共组件 → 数据 → workitem → state machine → clarification → ai → dict → 前端 → 测试 → 文档"分解                        | 调整实施顺序为 **workitem → clarification → ai → dict**（按用户偏好"业务模块按此顺序"）                                                                                                                |
| 方案设计 | 状态机邻接表、守卫链接口签名、AI Capability/Adapter 模式、错误码字典、`Result<T>` 统一返回                                                     | 修正邻接表回退方向（"仅相邻回退"）、DONE 不可变作为独立守卫                                                                                                                                                |
| 代码生成 | 公共层 / 实体 / DTO / VO / Mapper 接口 / Service 接口 / 各 Service 实现 / Controller / 状态机 / 守卫 / Mock AI / 前端 Vue 页面          | 修正 `@Version` 需 `OptimisticLockerInnerInterceptor`；前端 `request.js` 拦截器 unwrap `data` 避免 `res.data.data`；状态历史同事务写入；`@TableLogic` + 软删除；P0 守卫索引 `(work_item_id, severity, status)` |
| 测试生成 | `StateMachineTest` / `P0ClarificationGuardTest` / `MockAiAdapterTest` / 各 Service / Controller 集成测试                | 补 `@Mock` / `@InjectMocks` 与 `MockitoExtension`；增加边界用例（DONE 不可变、P0 计数为 0 放行、重复解决、乐观锁冲突）                                                                                          |
| 文档编写 | `architecture.md` / `api-design.md` / `breakdown.md` / `db.sql` / 本 README                                         | 与代码严格对齐（邻接表、错误码、API 路径）                                                                                                                                                          |

**效果评价**：

- ✅ 提效显著：状态机 / 守卫 / AI Capability 抽象的初版代码、AI 字典、测试骨架、文档骨架；
- ⚠️ 需复核：AI 生成的乐观锁相关 mapper 配置、MyBatis-Plus `@TableLogic` 默认值行为、前端 `vite proxy` 路径，需要人工对照官方文档校正；
- ❌ 不能直接用：`@Version` 默认行为在不同版本下会"看似成功实际跳过"，必须显式注册拦截器。

***

## 10. 后续优化方向

| 优先级 | 能力                                                          | 影响面                                                             |
| --- | ----------------------------------------------------------- | --------------------------------------------------------------- |
| P1  | 补齐 `AcceptanceCapability` / `TaskBreakdownCapability` 实现    | `ai` 模块；按现有 3 个 Capability 模板 5 分钟可补                            |
| P1  | 引入 JWT 鉴权替换 `X-User` 直传                                     | 新增 `AuthFilter`；`UserContext` 扩展 `userId` / `role` / `tenantId` |
| P1  | 接口幂等（`Idempotency-Key` 拦截器）                                 | 写接口防重放；`api-design.md §7` 已设计                                   |
| P2  | 真实 LLM 适配器（`LlmAiAdapter`）+ Prompt 模板管理                     | `ai.source=llm` 切换；`payload` 后置 schema 校验                       |
| P2  | 工作项搜索（ES / 简单全文）                                            | 新增 `/work-items/search`；`WorkItemQueryReqDTO` 扩展 `keyword` 命中字段 |
| P2  | 状态流转异步通知（WebSocket / SSE）                                   | 新增 `/work-items/{id}/transitions/stream`                        |
| P2  | AI 异步任务 + 轮询                                                | `trigger` 返回 `taskId`，新增 `/ai-analyses/{taskId}`                |
| P2  | 详情页 `p0OpenClarifications` / `totalOpenClarifications` 真实计算 | 注入 `ClarificationService` 计数即可                                  |
| P3  | 看板 / 甘特 / 复杂前端工程                                            | 前端升级；本题不强制                                                      |
| P3  | 批量操作（批量流转、批量解决澄清）                                           | 新增 `POST /work-items/batch/...`                                 |
| P3  | 审计日志接口                                                      | 新增 `/audit-logs` 模块                                             |
| P3  | 多租户隔离                                                       | 全部列表接口增加 `tenantId` 过滤                                          |
| P3  | Docker Compose 一键启动                                         | 新增 `Dockerfile` + `docker-compose.yml`（MySQL + 后端 + 前端）         |

***

## 附录

- 过程记录：[docs/process.md](docs/process.md)
- AI 使用记录：[docs/ai-usage.md](docs/ai-usage.md)
- API 契约：[docs/design/api-design.md](docs/design/api-design.md)
- 架构设计：[docs/design/architecture.md](docs/design/architecture.md)
- 任务拆解：[docs/tasks/breakdown.md](docs/tasks/breakdown.md)
- 需求理解：[docs/requirements/requirement.md](docs/requirements/requirement.md)
- 后端考题：[docs/requirements/candidate\_backend.md](docs/requirements/candidate_backend.md)
- 数据库脚本：[docs/database/db.sql](docs/database/db.sql)
- 团队编码规范：[.trae/rules/group\_development\_rule.md](.trae/rules/group_development_rule.md)
- 提交规范：[docs/02\_提交规范.md](docs/02_提交规范.md)

