# 日常 Java（Spring Boot + JDK 21 + MyBatis-Plus + MySQL8）+ 前端 Vue 项目分离团队开发编码规范

> 适用范围：公司内部中后台、业务系统、管理平台等前后端分离项目。
> 目标：统一代码风格、提升协作效率、降低联调成本、保证可维护性与可扩展性。

---

## 1. 总则

1. 代码以“可读、可维护、可测试、可扩展”为第一原则。
2. 所有功能必须先设计，再编码，再联调，再测试，再上线。
3. 后端负责业务规则、数据校验、权限控制、数据持久化；前端负责交互、展示、状态管理、用户体验。
4. 所有接口都必须有明确的请求/响应定义，不允许口头约定。
5. 任何新增功能必须遵循统一目录结构、命名规则、提交规范与测试要求。
6. 禁止在生产代码中留下调试输出、临时代码、无意义注释与硬编码配置。

---

## 2. 项目目录规范

### 2.1 后端目录结构（Spring Boot）

```text
backend
├── src/main/java/com/company/project
│   ├── common            # 通用常量、枚举、工具类、统一返回体
│   ├── config            # 配置类
│   ├── controller        # 接口层
│   ├── dto               # 请求入参对象
│   ├── entity            # 数据库实体
│   ├── enum              # 枚举定义
│   ├── exception         # 全局异常、业务异常
│   ├── mapper            # MyBatis-Plus Mapper
│   ├── service           # 业务接口
│   ├── service/impl      # 业务实现
│   ├── vo                # 响应对象
│   ├── util              # 工具类
│   └── Application.java
├── src/main/resources
│   ├── mapper            # xml 文件（如有）
│   ├── application.yml
│   └── application-dev.yml
└── src/test/java
```

### 2.2 前端目录结构（Vue）

```text
frontend
├── src
│   ├── api               # 接口请求封装
│   ├── assets            # 静态资源
│   ├── components        # 公共组件
│   ├── layout            # 布局组件
│   ├── router            # 路由
│   ├── store             # 状态管理（Pinia / Vuex）
│   ├── views             # 页面级组件
│   ├── utils             # 工具方法
│   ├── directives        # 自定义指令
│   ├── styles            # 全局样式
│   └── main.js / main.ts
├── public
└── vite.config.js / vue.config.js
```

---

## 3. 分层职责规范

### 3.1 后端分层职责

* **Controller**：只做参数接收、参数校验、调用 service、返回结果，不写复杂业务逻辑。
* **Service**：承载核心业务规则、事务处理、流程编排。
* **Mapper**：只负责数据库访问，不写业务逻辑。
* **Entity**：与数据库表一一对应。
* **DTO**：用于接收前端请求参数，避免直接暴露 Entity。
* **VO**：用于返回前端的数据结构，避免直接返回 Entity。

### 3.2 前端分层职责

* **View**：页面展示与页面级交互。
* **API**：统一封装接口调用。
* **Store**：管理跨页面共享状态。
* **Components**：复用组件沉淀。
* **Utils**：纯函数、格式化、校验等通用逻辑。

---

## 4. 命名规范

### 4.1 通用命名

1. 类名使用 **UpperCamelCase**，例如：`UserController`、`OrderServiceImpl`。
2. 方法名、变量名使用 **lowerCamelCase**，例如：`queryUserList`、`pageSize`。
3. 常量使用 **全大写 + 下划线**，例如：`DEFAULT_PAGE_SIZE`。
4. 包名全部小写，避免中文与特殊字符。
5. 禁止使用无意义缩写，如 `data1`、`tmp`、`test2`。

### 4.2 后端命名

* Controller：`xxxController`
* Service：`xxxService`
* Service 实现：`xxxServiceImpl`
* Mapper：`xxxMapper`
* Entity：`xxxEntity`
* DTO：`xxxReqDTO` / `xxxDTO`
* VO：`xxxRespVO` / `xxxVO`
* 表名：小写下划线，如 `sys_user`
* 字段名：小写下划线，如 `create_time`

### 4.3 前端命名

* 组件名：`UserTable.vue`
* 页面名：`UserList.vue`
* API 文件：`user.js` 或 `user.ts`
* 路由 name：使用业务含义，如 `UserManagement`
* Pinia store：`useUserStore`

---

## 5. 后端编码规范

### 5.1 Controller 规范

1. Controller 仅负责参数接收和返回，不允许堆积业务逻辑。
2. 接口路径必须语义清晰，建议使用 REST 风格。
3. 每个接口必须声明清晰的请求方式与路径。
4. 入参对象必须使用 DTO，不直接使用 Entity。
5. 返回值统一封装为标准响应体。

示例：

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Result<UserRespVO> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }
}
```

### 5.2 Service 规范

1. Service 层负责核心业务逻辑。
2. 涉及多个数据库操作时必须考虑事务。
3. 不允许 Service 直接返回 Entity 给前端。
4. 必要时将复杂逻辑拆分成私有方法，保持方法单一职责。
5. 业务异常统一抛出业务异常，不允许直接抛出大量底层异常到 Controller。

### 5.3 Mapper 规范

1. Mapper 只做数据访问。
2. 使用 MyBatis-Plus 能解决的查询优先使用内置能力。
3. 复杂查询才使用自定义 SQL。
4. SQL 必须可读、可维护，避免过度嵌套。
5. Mapper 方法命名必须语义明确，如 `selectUserById`、`pageUserList`。

### 5.4 Entity 规范

1. Entity 只描述数据库结构，不承载业务逻辑。
2. 必须与表字段保持一致。
3. 建议增加序列化标识与通用字段，如创建时间、更新时间、删除标识。
4. 关联字段、展示字段不要放入 Entity。

### 5.5 DTO / VO 规范

1. **DTO** 用于接收前端请求参数。
2. **VO** 用于返回前端结果。
3. Entity 不得直接暴露给前端。
4. DTO 和 VO 根据业务场景拆分，不要复用过度。

---

## 6. 接口设计规范

### 6.1 REST 风格建议

* 查询列表：`GET /api/user/list`
* 查询详情：`GET /api/user/{id}`
* 新增：`POST /api/user`
* 修改：`PUT /api/user/{id}`
* 删除：`DELETE /api/user/{id}`

### 6.2 接口返回格式

统一返回结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000
}
```

建议统一封装 `Result<T>`。

### 6.3 分页规范

分页接口必须统一以下字段：

* `pageNo`
* `pageSize`
* `total`
* `records`

返回示例：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "pageNo": 1,
    "pageSize": 10,
    "total": 100,
    "records": []
  }
}
```

### 6.4 参数校验

1. 所有外部入参必须校验。
2. 必填字段、长度、格式、范围必须明确。
3. 参数校验失败时返回统一错误信息。
4. 复杂校验优先放在 DTO + 校验注解中。

常用注解示例：

```java
@NotNull
@NotBlank
@Size(max = 50)
@Min(1)
@Max(100)
```

---

## 7. MyBatis-Plus 使用规范

1. 优先使用 MyBatis-Plus 提供的 CRUD 能力。
2. 对于简单分页、单表查询、条件查询，优先使用 `LambdaQueryWrapper`。
3. 禁止把大量 SQL 拼接写到业务代码中。
4. 复杂 SQL 放入 Mapper XML 或注解 SQL 中统一维护。
5. 更新时避免全字段覆盖，注意只更新必要字段。

示例：

```java
LambdaQueryWrapper<UserEntity> query = Wrappers.lambdaQuery();
query.eq(UserEntity::getStatus, 1)
     .like(StringUtils.isNotBlank(keyword), UserEntity::getUsername, keyword)
     .orderByDesc(UserEntity::getCreateTime);
```

### 7.1 分页查询规范

1. 分页查询必须显式指定排序规则。
2. 不允许无排序分页。
3. 大表查询必须关注索引与执行计划。

---

## 8. MySQL 规范

### 8.1 表设计

1. 表名统一小写下划线。
2. 主键建议使用 `bigint`。
3. 必须有创建时间、更新时间、逻辑删除字段（如业务需要）。
4. 字段命名要语义明确，避免缩写。
5. 数据类型选择要贴近业务，不滥用 `varchar(255)`。

### 8.2 索引规范

1. 高频查询条件必须建索引。
2. 联合索引遵循最左前缀原则。
3. 避免过多无效索引。
4. 变更前应评估索引对写入性能的影响。

### 8.3 SQL 规范

1. SQL 必须可读，关键字大写，字段小写下划线。
2. 禁止 `select *`。
3. 查询必须只取需要字段。
4. 批量操作注意分批提交，避免一次性大事务。

---

## 9. 前端 Vue 编码规范

### 9.1 组件规范

1. 一个组件只做一类事情。
2. 页面组件与通用组件分离。
3. 可复用 UI 逻辑优先抽成公共组件。
4. 不要在模板中堆过多复杂逻辑。
5. 组件参数通过 `props` 明确声明。

### 9.2 页面规范

1. 页面应有清晰的标题、筛选区、列表区、操作区。
2. 页面逻辑尽量保持轻量，复杂逻辑放到 `api`、`store`、`utils`。
3. 大页面应拆分为多个子组件。

### 9.3 API 调用规范

1. 所有接口请求统一封装。
2. 不允许在页面中直接写原始请求地址。
3. 请求参数和返回值都要定义类型。
4. 统一处理 loading、错误提示、空状态。

### 9.4 状态管理规范

1. 全局共享数据放入 store。
2. 页面局部状态放在组件内部。
3. 不要把所有状态都塞进全局 store。
4. store 的命名必须与业务一致。

### 9.5 样式规范

1. 优先使用统一设计体系或组件库。
2. 样式命名保持语义化。
3. 避免大量内联样式。
4. 公共样式放入全局样式文件。
5. 页面级样式建议局部作用域隔离。

---

## 10. 前后端联调规范

1. 接口文档必须先于开发完成或同步完成。
2. 前后端约定字段名、类型、分页格式、错误码。
3. 联调阶段必须使用统一环境配置。
4. 接口变更必须提前通知对方。
5. 不允许在未同步文档的情况下擅自改接口。

---

## 11. 代码注释规范

1. 注释只写“为什么”，不要重复代码表面含义。
2. 复杂业务流程必须加必要注释。
3. 重要规则、边界条件、兼容逻辑要说明。
4. 过时注释必须及时删除。
5. 不写废话注释，例如“获取用户信息”这种显而易见的描述。

---

## 12. 异常处理规范

### 12.1 后端异常处理

1. 使用统一全局异常处理器。
2. 业务异常与系统异常分开处理。
3. 不允许直接把异常堆栈返回给前端。
4. 错误信息需要清晰、可定位、便于排查。

### 12.2 前端异常处理

1. 请求失败要有统一提示。
2. 页面渲染异常要兜底处理。
3. 空数据、无权限、接口超时等场景要有明确展示。

---

## 13. 日志规范

1. 关键业务必须打日志。
2. 日志内容要能定位问题，但不要泄露敏感信息。
3. 禁止打印密码、token、身份证号等敏感字段。
4. 日志级别使用合理：

   * `INFO`：正常业务流程
   * `WARN`：可预期异常或风险
   * `ERROR`：系统异常

---

## 14. 安全规范

1. 所有接口必须考虑鉴权。
2. 敏感参数传输必须通过安全通道。
3. 前端不得保存敏感信息到不安全位置。
4. 后端必须校验权限，不依赖前端控制。
5. 防止 SQL 注入、XSS、CSRF 等常见安全问题。

---

## 15. 提交规范

建议使用 Conventional Commits：

```text
feat: 新增用户管理列表
fix: 修复分页查询异常
refactor: 重构订单创建逻辑
test: 补充用户接口单测
docs: 更新接口文档
chore: 调整构建配置
```

提交说明要求：

1. 一次提交只做一类事情。
2. 提交信息要能描述业务含义。
3. 不提交无关文件、临时文件、测试垃圾代码。

---

## 16. 分支与发布规范

1. `main/master`：稳定生产分支。
2. `develop`：日常集成分支。
3. `feature/*`：需求开发分支。
4. `hotfix/*`：线上紧急修复分支。
5. 每个需求建议独立分支开发，完成后合并。
6. 发布前必须完成自测、联调、回归测试。

---

## 17. 测试规范

### 17.1 后端测试

1. 核心业务必须有单元测试。
2. 关键接口建议有集成测试。
3. 测试数据尽量独立构造。
4. 边界场景必须覆盖：空值、异常值、权限不足、重复提交。

### 17.2 前端测试

1. 关键页面操作流程应至少完成手工回归验证。
2. 复杂组件建议补充组件测试。
3. 修复 bug 后必须验证原场景和关联场景。

---

## 18. 评审规范

1. 代码必须经过至少一次评审。
2. 评审关注点：设计是否合理、命名是否规范、边界是否完整、是否有重复代码、是否影响性能。
3. 评审不是挑毛病，而是保证整体质量。
4. 评审意见必须闭环处理。

---

## 19. 常见禁止项

1. 禁止在 Controller 中写复杂业务逻辑。
2. 禁止前端页面直接访问后端地址，不经过统一封装。
3. 禁止 Entity 直接透传到前端。
4. 禁止无意义复制粘贴代码。
5. 禁止随意修改公共字段和公共接口。
6. 禁止把临时代码带入正式分支。
7. 禁止不经过测试就提交合并。

---

## 20. 推荐落地流程

1. 需求评审
2. 接口与数据模型设计
3. 开发任务拆分
4. 后端先行定义接口与 DTO/VO
5. 前端同步 Mock 或联调
6. 后端实现业务与数据层
7. 前端实现页面与交互
8. 联调与问题修复
9. 测试与回归
10. 代码评审与发布

---

## 21. 附：统一命名示例

### 21.1 后端示例

```java
public class UserQueryReqDTO {
    private String username;
    private Integer status;
    private Integer pageNo;
    private Integer pageSize;
}

public class UserRespVO {
    private Long id;
    private String username;
    private Integer status;
    private String createTime;
}
```

### 21.2 前端示例

```javascript
// src/api/user.js
export function getUserList(params) {
  return request({
    url: '/api/user/list',
    method: 'get',
    params
  })
}
```

---

## 22. 结语

本规范用于统一团队协作方式，减少沟通成本，提升交付质量。
实际项目中可根据业务复杂度、团队规模、技术栈版本进行补充与调整。
