# MediAsk API 接口文档

> 此文档供 AI 读取以生成前端代码。

## 快速索引

| 模块 | 接口数量 | 说明 |
|------|----------|------|
| 认证 | 4 | 注册、登录、刷新Token、登出 |
| 用户 | 1 | 获取用户信息 |
| 排班 | 7 | 创建/查询排班、停诊/开诊 |
| 预约 | 8 | 预约挂号、取消、支付 |
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
// 1. 登录获取 Token
POST /api/v1/auth/login
Body: { account: string, password: string }
Response: {
  token: string,           // Access Token (30分钟有效)
  refreshToken: string,    // Refresh Token (30天有效)
  refreshTokenId: string,  // Refresh Token ID（登出时使用）
  expiresIn: number        // Access Token 剩余秒数
}

// 2. 后续请求携带 Access Token
Headers: { Authorization: "Bearer {token}" }

// 3. Access Token 过期后，使用 Refresh Token 刷新
POST /api/v1/auth/refresh
Body: { refreshToken: string }
Response: { token, refreshToken, refreshTokenId, expiresIn }

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

### User 用户模块 (1 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/users/me` | 获取当前用户信息 |

### Schedule 排班模块 (7 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/schedules` | 创建排班 |
| GET | `/api/v1/schedules` | 查询排班列表 |
| POST | `/api/v1/schedules/auto` | 自动排班 |
| POST | `/api/v1/schedules/{id}/close` | 停诊 |
| POST | `/api/v1/schedules/{id}/open` | 开诊 |
| PUT | `/api/v1/schedules/{id}/slots` | 调整号源 |
| GET | `/api/v1/schedules/available` | 可预约排班 |

### Appointment 预约模块 (8 接口)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/appointments` | 创建预约 |
| POST | `/api/v1/appointments/cancel` | 取消预约 |
| POST | `/api/v1/appointments/{id}/pay` | 支付预约 |
| POST | `/api/v1/appointments/{id}/visited` | 标记已就诊 |
| GET | `/api/v1/appointments/my` | 我的预约 |
| GET | `/api/v1/appointments/my/unpaid` | 待支付预约 |
| GET | `/api/v1/appointments/{id}` | 预约详情 |
| GET | `/api/v1/appointments/slots/available` | 可预约时段 |

## 文件说明

| 文件 | 用途 |
|------|------|
| `openapi.json` | OpenAPI 3.0 规范，AI 解析生成代码 |
| `README.md` | 此文件，接口快速索引 |

## 提示

- AI 读取 `openapi.json` 即可获得完整接口定义
- 文件按模块分组（注释标记），便于查找
- 后续新增模块可在 `openapi.json` 中按相同模式扩展
