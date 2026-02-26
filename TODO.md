# TODO.md

> MediAsk 项目改进任务清单
>
> **最后更新**: 2026-02-25
> **下一步建议**: 安全加固 或 AI/RAG 问诊模块

---

## 🎯 下一步建议（按优先级）

### 方案 A: 继续完善预约/排班模块（短期）

- [ ] **集成测试**
  - `AppointmentIntegrationTest` - 完整预约流程
  - `ScheduleIntegrationTest` - 完整排班流程
  - `AppointmentWorkflowTest` - 状态机流程

- [ ] **缓存层实现**
  - 医生排班 Redis 缓存
  - 号源信息缓存

- [ ] **API 响应增强**
  - 医生端患者信息查询
  - 预约统计接口

### 方案 B: 开始 AI/RAG 问诊模块（核心功能，中期）

- [ ] **领域服务**
  - `AiConversationDomainService` 领域服务
  - `RagService` 混合检索 (BM25 + Vector)

- [ ] **基础设施**
  - Milvus Repository 实现
  - LangChain4j + DeepSeek 集成

- [ ] **API 层**
  - `AiController` 问诊接口
  - 流式响应支持

### 方案 C: 修复安全问题（紧急）

- [ ] 修复注册越权漏洞
- [ ] 添加 Rate Limiting
- [ ] 移除硬编码密钥

---

## Code Review 遗留项 (2026-02-25 CR)

> 以下项目来自 `CR/code-review-2026-02-25-16-04-46.md`，本次未修复，需后续跟进。

### 需确认

- [ ] **[M-4] 确认错误码兼容性** — `AppointmentApplicationService.java:224`
  - `cancelAppointment` 中错误码从 `EMR_ACCESS_DENIED(5002)` 改为 `ACCESS_DENIED(1103)`
  - 语义修正正确（预约取消不属于病历模块），但属于面向客户端的 API 行为变更
  - **需与前端确认**是否对错误码 5002 有专门处理逻辑，若有需前后端同步发布
  - 在 API 变更日志中记录此 breaking change

- [ ] **[S-5] 确认 replaceUserRolesByCodes 事务保护** — `AuthzRepositoryImpl.java:195-227`
  - 该方法先 delete 所有旧角色关联再逐条 insert 新关联
  - 需确认调用方 `AuthzApplicationService.updateUserRoles` 的 `@Transactional` 能完全覆盖
  - 否则 delete 成功但 insert 失败会导致用户丧失所有角色

- [ ] **[m-6] EMR_ACCESS_DENIED 枚举是否还有其他使用方** — `ErrorCode.java:84`
  - 预约取消不再使用该枚举，如无其他引用建议标记 `@Deprecated`

### 架构优化

- [ ] **[m-3] Infra 层 Repository 抛 BizException 职责归属** — `AuthzRepositoryImpl.java:205-216`
  - 当前 `replaceUserRolesByCodes` 中参数空值校验在 Infra 层抛 `BizException`
  - 按 DDD 分层原则应移至 Service 层（AssertUtil 模式），Repository 层只负责数据级校验

- [ ] **[m-5] replaceUserRolesByCodes N+1 查询优化** — `AuthzRepositoryImpl.java:209-214`
  - `findRoleByCode` 对每个 roleCode 最多执行 3 次 DB 查询（原始、大写、小写）
  - 建议改为 IN 查询一次性获取所有角色

### 代码质量改进 (Suggestion 级别)

- [ ] **[S-1] 合并 BindException 分支消除重复** — `GlobalExceptionHandler.java`
  - `MethodArgumentNotValidException` 继承自 `BindException`，可合并为一个分支

- [ ] **[S-2] 补充 TraceIdFilter 类 Javadoc** — `TraceIdFilter.java`
  - 说明完整行为：读取/生成 traceId → 写入 MDC → 响应头回传 → 清理 MDC

- [ ] **[S-3] 补充 GlobalExceptionHandler 各方法 Javadoc** — `GlobalExceptionHandler.java`
  - 为每个 handler 方法添加简要说明（捕获场景和触发条件）

- [ ] **[S-4] 异常处理 handler 日志补充请求上下文** — `GlobalExceptionHandler.java`
  - warn/error 级别 handler 注入 `HttpServletRequest` 记录 URL、HTTP 方法、用户 ID 等
  - 或确认 logback 模板已包含 MDC requestUri 字段

---

## 🔴 测试覆盖

- [ ] **建立测试框架**
  - 启用 TestContainers 依赖
  - 添加 JUnit 5 + Mockito 配置

- [x] **Domain 层单元测试 - 预约模块** ✅
  - `Appointment` 聚合根测试 (16 tests)
  - `AppointmentStatus` 值对象测试 (28 tests)
  - `PatientId` 值对象测试 (7 tests)
  - `AppointmentId` 值对象测试 (6 tests)
  - 覆盖率: 57 tests passed ✅

- [x] **Domain 层单元测试 - 排班模块** ✅
  - `DoctorSchedule` 实体测试 (20 tests)
  - `AppointmentSlot` 时段测试 (7 tests)
  - `ScheduleStatus` 枚举测试 (12 tests)
  - 覆盖率: >85% ✅

### 🔴 核心功能开发

- [x] **预约挂号模块** ✅ 已完成
  - Domain 层: `Appointment` 聚合根, 值对象, 仓储接口, 领域事件
  - Application 层: `AppointmentApplicationService` 应用服务
  - Infra 层: `AppointmentRepositoryImpl`, `AppointmentConverter`
  - API 层: `AppointmentController` 控制器
  - 单元测试: 57 tests passed
  - SQL: `appointments`, `appointment_slots` 表及测试数据

- [ ] **AI/RAG 问诊模块** (下一步建议)
  - `AiConversationDomainService` 领域服务
  - `RagService` 混合检索 (BM25 + Vector)
  - Milvus Repository 实现
  - LangChain4j + DeepSeek 集成

---

## 中优先级 (Medium Priority)

### 🟡 集成测试

- [ ] `AppointmentIntegrationTest` - 完整预约流程测试
  - 创建预约 → 支付 → 就诊
  - 创建预约 → 30分钟未支付 → 自动取消
  - 同一患者同一天2个号源 → 允许
  - 同一患者同一天3个号源 → 拒绝

- [ ] `ScheduleIntegrationTest` - 完整排班流程测试
  - 创建排班 → 删除 → 检查关联预约

### 🟡 缓存层实现

- [ ] **医生排班 Redis 缓存**
  - 排班列表缓存（5分钟过期）
  - 缓存更新策略

- [ ] **号源信息缓存**
  - 可用号源实时查询

### 🟡 代码质量

- [ ] **修复 TODO 注释**
  - `ScheduleRule.java:69` - 节假日检查
  - `ScheduleConverter.java:62` - 从数据库读取 slot duration
  - `ScheduleApplicationService.java:180` - 同步调整时段数量
  - `ScheduleApplicationService.java:238` - 实现事件发布器

- [ ] **添加输入验证**
  - `LoginRequest` 添加 `@NotBlank`
  - 手机号格式验证 (正则)
  - 密码强度校验

### 🟡 技术债务

- [x] **修复 `@Data` 循环引用问题** ✅
  - `Appointment` 实体改用 `@Getter/@Setter`
  - 避免 `@ToString` 导致栈溢出

- [x] **使用项目现有基础设施** ✅
  - 分布式锁: 使用 `mediask-infra` 中已有的 `@DistributedLockable` 注解
  - 异常处理: 使用项目现有的 `BizException` 和 `ErrorCode`

- [x] **PageResult 统一重构** ✅
  - 在 mediask-common 创建统一 PageResult
  - 删除 mediask-service/api 中重复类

- [ ] **完善 SQL 初始化脚本**
  - 补充 doctors 表
  - 补充 medical_records 表
  - 补充 drug_prescriptions 表

- [ ] **修复代码问题**
  - Redis 序列化安全隐患

- [ ] **数据库索引优化**
  - `appointments` 表添加 `idx_patient_deleted`
  - `medical_records` 表添加 `idx_doctor_status`

---

## 低优先级 (Low Priority)

### 🟢 功能完善

- [ ] **病历管理模块**
  - `MedicalRecordApplicationService`
  - 病历 CRUD 操作
  - 版本控制集成

- [ ] **处方管理模块**
  - `PrescriptionApplicationService`
  - 药物相互作用检查
  - 配伍禁忌校验

- [ ] **医生/医院管理**
  - `DoctorApplicationService`
  - `HospitalApplicationService`
  - 科室管理

### 🟢 Worker 模块

- [x] **定时任务实现** ✅
  - AppointmentScheduler - 预约超时自动取消（每5分钟）
  - AppointmentScheduler - 爽约标记（每日12点）
  - ScheduleScheduler - 排班过期标记（每日凌晨1点）
  - ScheduleScheduler - 历史数据清理（每月1日）

- [ ] **MQ 消费者**
  - 预约确认通知
  - 知识库文档异步解析
  - 审计日志异步写入

### 🟢 性能优化

- [ ] **缓存层实现** (见中优先级)
  - 医生排班 Redis 缓存
  - 药品信息缓存
  - 用户权限缓存

- [ ] **异步处理**
  - 预约确认 MQ 通知
  - 知识库文档解析异步化

### 🟢 监控与日志

- [ ] **Actuator 健康检查**
  - `/actuator/health`
  - `/actuator/metrics`
  - `/actuator/prometheus`

- [ ] **结构化日志**
  - JSON 格式日志
  - 请求追踪 ID

---

## 已完成 ✅

- [x] 创建 CLAUDE.md 文件
- [x] 预约挂号模块 - Domain 层实现
- [x] 预约挂号模块 - Application 层实现
- [x] 预约挂号模块 - Infra 层实现
- [x] 预约挂号模块 - API Controller 实现
- [x] 预约挂号模块 - SQL 脚本及测试数据
- [x] 预约挂号模块 - 单元测试 (57 tests)
- [x] 修复 `@Data` 循环引用问题
- [x] 使用项目现有分布式锁基础设施

- [x] **预约挂号模块功能增强** (2026-01-18)
  - 医生查询预约列表接口
  - 预约冲突检查（同一天最多2个号源）
  - 爽约处理方法
  - 取消预约支持管理员角色

- [x] **排班模块功能增强**
  - 分页查询排班列表
  - 逻辑删除排班（检查关联预约）
  - 批量删除排班

- [x] **定时任务实现**
  - AppointmentScheduler（超时取消、爽约标记）
  - ScheduleScheduler（过期标记、历史清理）

- [x] **Domain 层测试补充**
  - DoctorScheduleTest (20 tests)
  - AppointmentSlotTest (7 tests)
  - ScheduleStatusTest (12 tests)

- [x] **API 文档同步**
  - 更新 openapi.json 新增接口
  - 更新 README.md 接口索引

- [x] **PageResult 统一重构**
  - 在 mediask-common 创建统一 PageResult
  - 删除 mediask-service/api 中重复类

- [x] **修复注册性别验证**
  - gender 字段改为必填

- [x] **Code Review 安全修复** (2026-02-25)
  - [C-1] TraceIdFilter traceId 输入校验（正则白名单+长度限制，防止日志注入）
  - [C-2] MDC 异步线程传播（新增 MdcTaskDecorator，eventTaskExecutor/scheduleSolverExecutor 均已配置）
  - [C-3] SysException handler 不再暴露内部错误消息，返回通用 SYSTEM_ERROR
  - [M-1] IllegalArgumentException handler 返回固定消息，error 级别记录堆栈
  - [M-2] 移除 IllegalStateException 全局拦截，走 handleOther 兜底
  - [M-3] 清理 AuthzApplicationService 死代码 catch(IllegalArgumentException) 块
  - [M-5] FilterRegistrationBean 禁用 TraceIdFilter Servlet 容器自动注册
  - [M-6] 新增 HttpRequestMethodNotSupported/HttpMediaTypeNotSupported 异常处理
  - [M-7] 校验异常返回所有错误信息（Collectors.joining）
  - [M-8] 修正 SecurityConfig traceIdFilter 字段缩进
  - [m-2] ConstraintViolation null 安全检查
  - [m-4] handleOther 移到类最末尾

---

## 阶段划分建议

```
阶段一: 基础设施 (1-2 周)
├── 安全加固 (Rate Limiting + 密钥管理)
├── 测试框架搭建
└── 基础单元测试

阶段二: 核心功能 (3-4 周)
├── AI/RAG 问诊模块
├── 预约挂号模块 ✅ 已完成
├── 排班管理模块 ✅ 已完成
└── 病历管理模块

阶段三: 完善优化 (2 周)
├── 处方配伍检查
├── Worker 定时任务 ✅ 已完成
└── 监控指标
```

---

## 项目统计

| 指标 | 数值 |
|------|------|
| 预约模块 Domain 层文件 | 8 |
| 预约模块 Service 层文件 | 6 |
| 预约模块 Infra 层文件 | 2 |
| 预约模块 API 层文件 | 1 |
| 单元测试数量 | 96+ |
| 测试通过率 | 100% |
| API 接口数量 | 35+ |
| 定时任务数量 | 4 |
| 完成功能模块 | 2 (预约 + 排班) |

## 最近更新

| 日期 | 更新内容 |
|------|----------|
| 2026-02-25 | Code Review 安全修复（C-1/C-2/C-3 + M-1~M-8 + m-2/m-4，共12项） |
| 2026-01-18 | 预约/排班功能增强、定时任务、测试覆盖、API文档同步 |
| 2026-01-18 | PageResult 统一重构、注册性别验证修复 |
| 2025-12-17 | 预约挂号模块核心实现 |
