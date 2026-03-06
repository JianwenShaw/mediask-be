# API 变更日志（面向前端 AI 对接）

> 目标：精确记录“某次提交到底改了哪些接口、改了什么、前端要怎么改”。

## 1. 记录规则（强约束）

- 每个提交（commit）新增一个独立条目，不跨提交合并。
- 每个条目必须包含：`commit`、`date`、`author`、`scope`、`breaking`、`endpoints`。
- endpoint 级别必须明确：`method`、`path`、`change_type`、`frontend_action`。
- 无法 100% 确认的内容必须写入 `notes`，并标注 `uncertain`。
- 只描述“代码中实际发生”的变化，不写推测性需求。

## 2. 标准模板（复制即用）

```markdown
## [提交] <commit-sha-7> - <yyyy-mm-dd>

metadata:
  author: <name>
  scope: <模块，如 auth/schedule/appointment>
  breaking: <yes|no>
  openapi_synced: <yes|no>
  readme_synced: <yes|no>

summary:
  - <一句话说明这次提交对接口层的核心影响>

endpoints:
| method | path | change_type | frontend_action | details |
|--------|------|-------------|-----------------|---------|
| GET | /api/v1/xxx | modified | update-types | <参数/响应/鉴权变化>

notes:
  - status: <confirmed|uncertain>
    content: <补充说明，若 uncertain 必写影响范围>
```

## 3. 字段枚举

- `change_type`：`added` | `modified` | `deprecated` | `removed`
- `frontend_action`：
  - `no-change`：前端无需改动
  - `update-types`：更新接口类型定义
  - `update-auth`：调整鉴权/Token 处理
  - `update-request`：调整请求参数或请求体
  - `update-response`：调整响应解析字段
  - `update-flow`：调整业务流程（如先预检再发布）

## 4. 变更记录

## [提交] uncommitted-working-tree - 2026-03-05

metadata:
  author: AI Agent
  scope: api-docs
  breaking: no
  openapi_synced: yes
  readme_synced: yes

summary:
  - 对齐当前 Controller 实现，修复文档鉴权、登出口径、类型精度与可生成性，降低前端 AI 对接歧义。

endpoints:
| method | path | change_type | frontend_action | details |
|--------|------|-------------|-----------------|---------|
| POST | /api/v1/auth/login | modified | update-auth | 显式声明公开接口（security=[]） |
| POST | /api/v1/auth/register | modified | update-auth | 显式声明公开接口（security=[]） |
| POST | /api/v1/auth/refresh | modified | update-auth | 显式声明公开接口（security=[]） |
| POST | /api/v1/auth/logout | modified | update-request | 明确当前实现不读取请求体，按 Bearer token 登出 |
| GET | /api/test/health | modified | update-auth | 显式声明公开接口（security=[]） |
| GET/POST | /api/test/mysql | modified | update-request | query 参数改为非必填并补默认值 |
| GET/POST | /api/test/redis | modified | update-request | query 参数改为非必填并补默认值 |
| GET | /api/test/all | modified | update-response | 返回模型纠正为通用 Result 包装 |
| POST | /api/v1/ai/reviews | modified | update-response | 响应由泛型包装细化为 AiReviewResultResponse |
| GET | /api/v1/ai/metrics/overview | modified | update-response | 响应细化为 AiOverviewMetricsResponse |
| GET | /api/v1/ai/metrics/departments | modified | update-response | 响应细化为 AiDepartmentMetricsListResponse |
| GET | /api/v1/users/me | modified | update-response | 响应细化为 UserResultResponse |
| GET | /api/v1/department/departments | modified | no-change | 增加说明：当前 Controller 为占位实现，data 可能为 null |

notes:
  - status: confirmed
    content: openapi 已补充 operationId，便于前端 SDK/类型生成。
  - status: confirmed
    content: ResponseWrapper 增加 traceId/timestamp 字段描述，与 Result<T> 当前实现一致。
  - status: uncertain
    content: DepartmentController 后续若补齐真实服务返回，前端需从 data=null 兼容切换为列表渲染。
