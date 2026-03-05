# MediAsk

> 基于大语言模型的智能医疗辅助问诊系统 - 后端服务

[![CI](https://github.com/JianwenShaw/mediask-be/actions/workflows/ci.yml/badge.svg)](https://github.com/JianwenShaw/mediask-be/actions/workflows/ci.yml)
[![Release](https://github.com/JianwenShaw/mediask-be/actions/workflows/release.yml/badge.svg)](https://github.com/JianwenShaw/mediask-be/actions/workflows/release.yml)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 项目简介

MediAsk 是一个集成传统医疗业务流程与 AI 辅助功能的智能问诊系统，通过 RAG（检索增强生成）技术解决大模型在医疗领域的"幻觉"问题，提供智能导诊、预问诊、辅助诊疗等功能。

### 核心特性

- 🤖 **AI 智能问诊** - 基于 LangChain4j + DeepSeek 的多轮对话预问诊
- 📚 **RAG 知识库** - 医疗指南向量化检索，确保回答基于权威来源
- 🏥 **智慧挂号** - 高并发号源管理，Redis 分布式锁防超卖
- 📋 **电子病历** - 结构化病历模板，处方配伍禁忌校验
- 🔐 **安全认证** - Spring Security + JWT 无状态认证

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| **语言** | Java | 21 (Virtual Threads) |
| **框架** | Spring Boot | 3.5.8 |
| **ORM** | MyBatis-Plus | 3.5.15 |
| **数据库** | MySQL | 8.0+ |
| **缓存** | Redis (Redisson) | 7.x |
| **向量库** | Milvus | 2.4+ |
| **AI** | LangChain4j | 1.9.1 |
| **文档** | Knife4j (OpenAPI 3) | 4.5.0 |

## 项目结构

```
mediask-be/
├── mediask-api/        # 接入层 - Web 入口、Controller、认证
├── mediask-service/    # 服务层 - 业务编排、事务管理
├── mediask-domain/     # 领域层 - 核心业务规则、实体
├── mediask-dal/        # 数据访问层 - Mapper、Repository 实现
├── mediask-common/     # 通用层 - 工具类、异常、常量
├── mediask-worker/     # 任务层 - 定时任务
└── MediAskDocs/        # 项目文档
```

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.x
- Docker (可选)

### 本地开发

1. **克隆仓库**

```bash
git clone https://github.com/JianwenShaw/mediask-be.git
cd mediask-be
```

2. **配置数据库**

```bash
# 创建数据库
mysql -uroot -p -e "CREATE DATABASE mediask CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入初始化 SQL
mysql -uroot -p mediask < mediask-dal/src/main/resources/sql/init-dev.sql
```

3. **修改配置**

编辑 `mediask-api/src/main/resources/application-dev.yml`，配置数据库连接信息。

4. **启动应用**

```bash
mvn spring-boot:run -pl mediask-api
```

5. **访问接口文档**

```
http://localhost:8989/doc.html
```

### Docker 部署

```bash
# 构建镜像
docker build -f Dockerfile.api -t mediask-api:latest .

# 运行容器
docker run -d \
  --name mediask-api \
  -p 8989:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  mediask-api:latest
```

## CI/CD

项目使用 GitHub Actions 实现自动化构建和部署：

| Workflow | 触发条件 | 功能 |
|----------|---------|------|
| `ci.yml` | Push/PR 到 master/dev | 构建、测试、失败时邮件通知 |
| `release.yml` | 推送语义化版本 Tag (v*.*.*) | 运行 CI → 构建 Docker 镜像 → 推送到 GHCR |
| `notify.yml` | 被其他工作流调用 | 可复用的邮件通知模块 |

### 分支保护规则

建议为 `master` 分支配置保护规则：

**Settings → Branches → Add branch protection rule**

| 设置项 | 推荐值 |
|--------|--------|
| Branch name pattern | `master` |
| Require a pull request before merging | ✅ |
| Require status checks to pass before merging | ✅ |
| **Status checks that are required** | `Build & Test` |
| Require branches to be up to date before merging | ✅ |

> ⚠️ Status check 名称是 `Build & Test`（ci.yml 中 job 的 `name` 字段）

### 发布流程

```bash
# 1. 确保代码已合并到 master
git checkout master && git pull

# 2. 创建语义化版本标签
git tag v1.0.0

# 3. 推送标签触发 Release
git push origin v1.0.0
```

Release 工作流会自动：
1. 运行完整的 CI 测试套件
2. 构建 Docker 镜像
3. 推送镜像到 GHCR，并生成以下标签：

| 标签格式 | 示例 | 说明 |
|----------|------|------|
| `{version}` | `1.0.0` | 完整版本号 |
| `{major}.{minor}` | `1.0` | 主次版本（自动获取最新 patch） |
| `{major}` | `1` | 主版本（v0.x.x 除外） |
| `latest` | - | 最新正式版（prerelease 不更新） |
| `sha-{hash}` | `sha-abc1234` | Git commit SHA |

> 💡 支持 prerelease 版本：`v1.0.0-beta.1`、`v1.0.0-rc.1` 等

### 拉取镜像

```bash
# 拉取最新稳定版
docker pull ghcr.io/jianwenshaw/mediask-be-api:latest

# 拉取指定版本
docker pull ghcr.io/jianwenshaw/mediask-be-api:1.0.0
```

### 邮件通知

CI 和 Release 工作流都支持邮件通知：

| 工作流 | 成功通知 | 失败通知 |
|--------|---------|---------|
| CI | ❌ | ✅ |
| Release | ✅ | ✅ |

**配置 Repository Secrets：**

在 GitHub 仓库 → Settings → Secrets and variables → Actions 中添加：

| Secret | 说明 | 示例 |
|--------|------|------|
| `MAIL_SERVER` | SMTP 服务器地址 | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP 端口 | `587` |
| `MAIL_USERNAME` | 发件人邮箱 | `ci-bot@example.com` |
| `MAIL_PASSWORD` | 邮箱密码/应用密码 | `xxxx xxxx xxxx xxxx` |
| `MAIL_TO` | 收件人邮箱（多个用逗号分隔） | `dev@example.com` |

> 💡 **Gmail 用户**：需要开启两步验证并生成[应用专用密码](https://myaccount.google.com/apppasswords)

## 文档

详细文档位于 `MediAskDocs/` 目录：

- [系统架构概览](MediAskDocs/docs/01-ARCHITECTURE_OVERVIEW.md)
- [代码规范](MediAskDocs/docs/02-CODE_STANDARDS.md)
- [配置管理](MediAskDocs/docs/03-CONFIGURATION.md)
- [部署运维](MediAskDocs/docs/04-DEVOPS.md)
- [测试策略](MediAskDocs/docs/05-TESTING.md)
- [数据库设计](MediAskDocs/DATABASE_DESIGN.md)

## 开发规范

- 遵循 [Alibaba Java 开发规范](https://github.com/alibaba/p3c)
- 提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)
- 代码格式化使用项目内置的 EditorConfig

## License

[MIT](LICENSE)
