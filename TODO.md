# TODO.md

> MediAsk 项目改进任务清单

## 高优先级 (High Priority)

### 🔴 安全问题

- [ ] **添加 Rate Limiting 接口限流**
  - 所有公开端点添加 Redis-based 限流
  - 防止暴力破解、恶意刷接口

- [ ] **移除硬编码密钥**
  - `application.yml` 默认密钥改为环境变量
  - `application-dev.yml` 数据库/Redis 密码改为环境变量
  - 添加启动时密钥验证

- [ ] **保护 Druid 控制台**
  - dev profile 下启用，prod 强制禁用
  - 修改默认账号密码

- [ ] **移除/保护 TestController**
  - 生产环境禁用 `/api/test/*` 端点
  - 或添加 IP 白名单

- [ ] **修复注册接口越权漏洞**
  - 目前 `userType` 参数可任意指定，任何人都能注册管理员
  - **临时方案**: 开发完成后删除 `/api/v1/auth/register` 公开注册接口
  - **长期方案**: 注册需要邀请码，或管理员审批

### 🔴 测试覆盖

- [ ] **建立测试框架**
  - 启用 TestContainers 依赖
  - 添加 JUnit 5 + Mockito 配置

- [x] **Domain 层单元测试 - 预约模块**
  - `Appointment` 聚合根测试 (16 tests)
  - `AppointmentStatus` 值对象测试 (28 tests)
  - `PatientId` 值对象测试 (7 tests)
  - `AppointmentId` 值对象测试 (6 tests)
  - 覆盖率: 57 tests passed ✅

- [ ] **Domain 层单元测试 - 排班模块**
  - `DoctorSchedule` 实体测试
  - `TimePeriod` 值对象测试
  - `ScheduleRule` 规则测试
  - 目标覆盖率: >70%

### 🔴 核心功能开发

- [ ] **AI/RAG 问诊模块**
  - `AiConversationDomainService` 领域服务
  - `RagService` 混合检索 (BM25 + Vector)
  - Milvus Repository 实现
  - LangChain4j + DeepSeek 集成

- [x] **预约挂号模块** ✅ 已完成
  - Domain 层: `Appointment` 聚合根, 值对象, 仓储接口, 领域事件
  - Application 层: `AppointmentApplicationService` 应用服务
  - Infra 层: `AppointmentRepositoryImpl`, `AppointmentConverter`
  - API 层: `AppointmentController` 控制器
  - 单元测试: 57 tests passed
  - SQL: `appointments`, `appointment_slots` 表及测试数据

---

## 中优先级 (Medium Priority)

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

- [ ] **完善 SQL 初始化脚本**
  - 补充 doctors 表
  - 补充 appointments 表
  - 补充 medical_records 表
  - 补充 drug_prescriptions 表

- [ ] **修复代码问题**
  - `DoctorSchedule.java` 重复 import
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

- [ ] **定时任务实现**
  - 过期排班自动标记
  - 预约过期取消

- [ ] **MQ 消费者**
  - 预约确认通知
  - 知识库文档异步解析
  - 审计日志异步写入

### 🟢 性能优化

- [ ] **缓存层实现**
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
└── 病历管理模块

阶段三: 完善优化 (2 周)
├── 处方配伍检查
├── Worker 定时任务
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
| 单元测试数量 | 57 |
| 测试通过率 | 100% |
