---
name: api-doc-sync
description: 同步并校验 mediask-api 控制器接口与 api-docs/openapi.json、api-docs/README.md 的一致性。用于新增/修改 REST API 后，快速发现 method/path 漂移、README 接口数量不准、路径参数名不一致，并可自动修复 README 的确定性问题。
---

# API 文档同步技能

在仓库根目录执行。

## 执行流程

1. 运行校验，检查 Controller 与 OpenAPI/README 的差异：

```bash
python3 skills/api-doc-sync/scripts/sync_api_docs.py --check
```

2. 若仅 README 有确定性问题（数量或 `{id}` 参数名），执行自动修复：

```bash
python3 skills/api-doc-sync/scripts/sync_api_docs.py --write
```

3. 再次校验确认无误：

```bash
python3 skills/api-doc-sync/scripts/sync_api_docs.py --check
```

## 脚本能力边界

- 自动校验：
  - `mediask-api` Controller 的 `method + path`
  - `api-docs/openapi.json` 的 `paths`
  - `api-docs/README.md` 的模块接口数量与关键路径参数名
- 自动修复（仅 README）：
  - 快速索引中的模块接口数量
  - 模块标题中的接口数量
  - `/api/v1/schedules/*` 与 `/api/v1/appointments/*` 的 `{id}` 占位符
- 不自动修复：
  - `openapi.json` 的请求/响应 schema、字段注释、示例语义

## 建议工作方式

- 每次改 Controller 后先跑 `--check`。
- 如果仅 README 漂移，先 `--write` 再 `--check`。
- 如果 OpenAPI 漂移，先同步 `openapi.json`，再回到 `--check`。
