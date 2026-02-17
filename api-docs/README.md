# MediAsk API 接口文档

> 此文档供 AI 读取以生成前端代码。

## 快速索引

| 模块 | 接口数量 | 说明 |
|------|----------|------|
| 认证 | 4 | 注册、登录、刷新Token、登出 |
| 用户 | 1 | 获取用户信息 |
| 权限管理 | 4 | 角色查询、权限查询、用户角色查询与更新 |
| 医生管理 | 5 | 医生档案创建、更新、启停、详情、分页 |
| 科室数据 | 1 | 查询科室列表 |
| 排班 | 12 | 创建/查询/删除排班、按模板生成排班 |
| 排班方案 | 4 | 方案版本查询、预检、发布、回滚 |
| 排班模板 | 4 | 创建/更新/查询/发布模板 |
| 预约 | 11 | 预约挂号、取消、支付、爽约、医生查询 |
| AI指标 | 3 | 医生复核、管理员总览、分科室统计 |
| 测试 | 5 | 健康检查、MySQL、Redis |

## 基础信息

| 项目 | 值 |
|------|-----|
| 服务地址 | `http://localhost:8989` |
| API 前缀 | `/api/v1` |
| 认证方式 | Bearer Token (JWT) |
| Access Token 有效期 | 30 分钟 |
| Refresh Token 有效期 | 30 天（存储在 Redis，可实时撤销） |

## 统一响应格式

```json
{
  "code": 0,           // 0=成功，非0=失败
  "msg": "success",    // 提示信息
  "data": { ... }      // 响应数据
}
```

## 认证流程

```typescript
// 1. 登录获取 Token（注意：认证字段在 data 内）
POST /api/v1/auth/login
Body: { account: string, password: string }
Response: {
  code: 0,
  msg: "success",
  data: {
    token: string,           // Access Token (30分钟有效)
    refreshToken: string,    // Refresh Token (30天有效)
    refreshTokenId: string,  // Refresh Token ID（登出时使用）
    expiresIn: number        // Access Token 剩余秒数
  }
}

// 2. 后续请求携带 Access Token
Headers: { Authorization: "Bearer {token}" }

// 3. Access Token 过期后，使用 Refresh Token 刷新
POST /api/v1/auth/refresh
Body: { refreshToken: string }
Response: { code, msg, data: { token, refreshToken, refreshTokenId, expiresIn } }

// 4. 登出（撤销 Refresh Token）
POST /api/v1/auth/logout
Headers: { Authorization: "Bearer {token}" }
Body: { refreshTokenId: string }  // 可选，为空则登出所有设备
```

## 模块概览

### Auth 认证模块 (4 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/auth/register` | 用户注册 |
| POST | `/api/v1/auth/login` | 用户登录 |
| POST | `/api/v1/auth/refresh` | 刷新 Token |
| POST | `/api/v1/auth/logout` | 用户登出 |

**登出请求示例**：
```json
// 登出当前设备
{ "refreshTokenId": "550e8400-e29b-41d4-a716-446655440000" }

// 登出所有设备
{}
```

**注册说明**：
- `POST /api/v1/auth/register` 注册成功后会直接返回登录态（`data` 内含 `token`、`refreshToken`、`refreshTokenId`、`expiresIn`），无需再调用一次登录接口。
- `gender` 为必填字段；`realName`、`birthDate`、`avatarUrl`、`phone` 为可选字段。

### User 用户模块 (1 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/users/me` | 获取当前用户信息 |

### Authz 权限管理模块 (4 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/authz/roles` | 查询角色列表（含权限编码） |
| GET | `/api/v1/admin/authz/permissions` | 查询权限列表 |
| GET | `/api/v1/admin/authz/users/{userId}/roles` | 查询用户角色 |
| PUT | `/api/v1/admin/authz/users/{userId}/roles` | 覆盖更新用户角色 |

**说明**：
- 以上接口仅管理员可访问（`hasAuthority('admin')`）。
- `PUT /api/v1/admin/authz/users/{userId}/roles` 为覆盖式更新，提交的 `roleCodes` 会替换用户当前角色集合。

### Doctor 医生管理模块 (5 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/doctors` | 创建医生档案 |
| PUT | `/api/v1/doctors/{doctorId}` | 更新医生档案 |
| PUT | `/api/v1/doctors/{doctorId}/status` | 启停医生档案 |
| GET | `/api/v1/doctors/{doctorId}` | 查询医生详情 |
| GET | `/api/v1/doctors` | 分页查询医生档案 |

### Department 科室数据模块 (1 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/department/departments` | 查询科室列表 |

### Schedule 排班模块 (12 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/schedules` | 创建排班 |
| GET | `/api/v1/schedules` | 分页查询排班列表（新增分页和筛选参数） |
| POST | `/api/v1/schedules/auto` | 自动排班 |
| POST | `/api/v1/schedules/generate` | 按模板生成排班实例（新增） |
| POST | `/api/v1/schedules/{scheduleId}/close` | 停诊 |
| POST | `/api/v1/schedules/{scheduleId}/open` | 开诊 |
| PUT | `/api/v1/schedules/{scheduleId}/slots` | 调整号源 |
| GET | `/api/v1/schedules/{scheduleId}` | 查询排班详情 |
| DELETE | `/api/v1/schedules/{scheduleId}` | 删除排班（新增） |
| GET | `/api/v1/schedules/doctor/{doctorId}` | 医生排班列表 |
| GET | `/api/v1/schedules/available` | 可预约排班 |
| DELETE | `/api/v1/schedules/batch` | 批量删除排班（新增） |

**创建排班真实口径（以当前代码为准）**：
- 请求字段为 `doctorId`、`scheduleDate`、`timePeriodCode`、`totalSlots`、`slotDurationMinutes`（不是 `date/periodCode`）。
- 同医生 + 同日期 + 同时段创建会被拒绝，返回业务异常 `code=1006`，提示“该时段的排班已存在”。
- `timePeriodCode` 仅支持 `1/2/3`（上午/下午/晚上）。

**排班状态与操作规则**：
- 状态码：`0=停诊`、`1=开放`、`2=约满`、`3=过期`。
- `POST /api/v1/schedules/{scheduleId}/close`、`POST /api/v1/schedules/{scheduleId}/open` 为幂等操作。
- 已过期排班不可开诊（open）。
- 删除排班时：`force=false` 且存在未取消预约会失败；`force=true` 会先取消关联预约再关闭排班。

**分页查询限制说明**：
- `GET /api/v1/schedules` 当前版本中 `departmentId` 参数暂未生效（已预留参数，后续版本补齐服务层过滤）。

**自动排班请求说明（已升级为科室多医生联合排班）**：
- `departmentId`：科室 ID（必填）
- `dateRange.startDate/endDate`：排班范围（必填）
- `periods`：参与求解的时段编码列表（必填）
- `doctorIds`：可选，指定医生池；为空则取科室全部在职医生
- `demand.byDatePeriod`：可选，按日期+时段的需求覆盖
- `hardConstraints`：可选，硬约束
- `hardConstraints.holidayPolicy`：节假日策略（`CLOSE/REDUCED/NORMAL`，推荐 `REDUCED`）
- `hardConstraints.holidayReductionFactor`：节假日降载系数（仅 `REDUCED` 生效，建议 0.4~0.7）
- `softGoals`：可选，软目标权重
- `solverConfig`：可选，求解策略参数

**自动排班响应说明**：
- `data.planId`：本次排班方案 ID
- `data.generatedScheduleIds`：当前实现通常为空（`POST /api/v1/schedules/auto` 仅保存 DRAFT 方案，不直接创建正式排班）
- `data.scoreSummary`：总分、硬约束违例数、软目标分解
- `data.explanations`：每个排班分配的解释与惩罚项
- `data.unfilledSlots`：未排满时段及原因
- `data.warnings`：告警信息（如时段未排满、已存在排班被跳过）
- 重要：`POST /api/v1/schedules/auto` 仅生成并保存 `DRAFT` 方案，不会直接生效；需调用 `POST /api/v1/schedule-plans/{planId}/publish` 才会落地到正式排班。

### ScheduleTemplate 排班模板模块 (4 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/schedule-templates` | 创建排班模板（新增） |
| PUT | `/api/v1/schedule-templates/{templateId}` | 更新排班模板（新增） |
| GET | `/api/v1/schedule-templates/{templateId}` | 查询排班模板详情（新增） |
| POST | `/api/v1/schedule-templates/{templateId}/publish` | 发布排班模板（新增） |

### SchedulePlan 排班方案模块 (4 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/schedule-plans/{planCode}/versions` | 查询方案版本列表 |
| GET | `/api/v1/schedule-plans/{planId}/precheck?mode=STRICT|FORCE` | 发布前冲突预检（不执行发布） |
| POST | `/api/v1/schedule-plans/{planId}/publish?mode=STRICT|FORCE` | 发布指定方案版本 |
| POST | `/api/v1/schedule-plans/{planId}/rollback?mode=STRICT|FORCE` | 回滚到指定方案版本（重新发布） |

**发布模式说明**：
- `STRICT`（默认）：只要检测到“已有有效预约导致无法替换”的冲突，直接拒绝发布。
- `FORCE`：允许发布，保留冲突排班并在返回 `warnings` 中给出冲突清单。

### Appointment 预约模块 (11 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/appointments` | 创建预约 |
| POST | `/api/v1/appointments/cancel` | 取消预约（支持管理员） |
| POST | `/api/v1/appointments/{appointmentId}/pay` | 支付预约 |
| POST | `/api/v1/appointments/{appointmentId}/visited` | 标记已就诊 |
| POST | `/api/v1/appointments/{appointmentId}/absent` | 标记爽约（新增） |
| GET | `/api/v1/appointments/my` | 我的预约 |
| GET | `/api/v1/appointments/my/unpaid` | 待支付预约 |
| GET | `/api/v1/appointments/{appointmentId}` | 预约详情 |
| GET | `/api/v1/appointments/slots/available` | 可预约时段 |
| GET | `/api/v1/appointments/doctor/{doctorId}` | 医生查询预约（按日期）（新增） |
| GET | `/api/v1/appointments/doctor/{doctorId}/range` | 医生查询预约（日期范围）（新增） |

### AI 指标模块 (3 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/ai/reviews` | 医生提交AI问诊复核 |
| GET | `/api/v1/ai/metrics/overview` | 管理员查看AI总览指标 |
| GET | `/api/v1/ai/metrics/departments` | 管理员查看AI分科室指标 |

## 文件说明

| 文件 | 用途 |
|------|------|
| `openapi.json` | OpenAPI 3.0 规范，AI 解析生成代码 |
| `README.md` | 此文件，接口快速索引 |

## 提示

- AI 读取 `openapi.json` 即可获得完整接口定义
- 文件按模块分组（注释标记），便于查找
- 后续新增模块可在 `openapi.json` 中按相同模式扩展

## 接口变更记录（供前端对接跟踪）

说明：
- 仅记录“新增/修改/删除”的接口变更，按时间倒序追加。
- 前端完成联调后，可将“对接状态”从 `待对接` 改为 `已对接` 并补充备注。

| 日期 | 变更类型 | 方法 | 路径 | 模块 | 对接状态 | 备注 |
|------|----------|------|------|------|----------|------|
| 2026-02-16 | 新增 | GET | `/api/v1/department/departments` | 科室数据 | 待对接 | 同步 Controller 到 OpenAPI，补齐缺失接口 |
| 2026-02-16 | 修改 | POST | `/api/v1/schedules` | 排班 | 待对接 | 创建排班请求字段口径修正为 `scheduleDate/timePeriodCode` |
| 2026-02-16 | 修改 | GET | `/api/v1/schedules` | 排班 | 待对接 | 明确 `departmentId` 当前版本暂未生效 |
| 2026-02-16 | 修改 | POST | `/api/v1/schedules/auto` | 排班 | 待对接 | 明确仅保存 DRAFT，`generatedScheduleIds` 通常为空 |
| 2026-02-12 | 新增 | GET | `/api/v1/admin/authz/roles` | 权限管理 | 待对接 | 查询角色列表（含权限编码） |
| 2026-02-12 | 新增 | GET | `/api/v1/admin/authz/permissions` | 权限管理 | 待对接 | 查询权限列表 |
| 2026-02-12 | 新增 | GET | `/api/v1/admin/authz/users/{userId}/roles` | 权限管理 | 待对接 | 查询用户角色 |
| 2026-02-12 | 新增 | PUT | `/api/v1/admin/authz/users/{userId}/roles` | 权限管理 | 待对接 | 覆盖更新用户角色 |
