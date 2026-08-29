# IntelliFlow AI — 智能对话平台

<div align="center">

  <img src="https://img.shields.io/badge/Vue-3.4-42b883?logo=vuedotjs" alt="Vue 3.4"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=springboot" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk" alt="Java 17"/>
  <img src="https://img.shields.io/badge/AgentScope-1.0.12-8b5cf6" alt="AgentScope"/>
  <img src="https://img.shields.io/badge/AG--UI-协议-0078d4" alt="AG-UI"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License"/>
  <a href="https://www.orcarouter.ai/ref/ref_bdd09e1fa977b44ced76"><img src="https://img.shields.io/badge/Powered_by-OrcaRouter-2563eb" alt="Powered by OrcaRouter"/></a>

  <h3>ReAct 智能体 · 工具调用 · 双层记忆 · 流式对话</h3>
  <p>一个面向学习与中小团队场景的全栈 AI 智能体平台</p>
</div>

---

## 项目简介

IntelliFlow AI 是一个基于 **AgentScope Java** 框架的全栈 AI 智能体平台。后端通过 ReActAgent 驱动模型进行「思考 → 调用工具 → 观察 → 回答」的推理循环，前端通过 **AG-UI 协议**实时展示思考过程与工具执行状态，体验接近 DeepSeek / ChatGPT 等产品。

## 功能一览

| 功能 | 状态 | 描述 |
|------|------|------|
| **ReAct 智能体** | ✅ | 基于 AgentScope ReActAgent，思考-行动循环，最多 10 轮工具调用 |
| **AG-UI 流式协议** | ✅ | 前端实时渲染推理过程、工具调用参数与结果（可折叠时间线） |
| **代码沙盒** | ✅ | Agent 可执行 Python / JavaScript / Shell 代码（危险命令黑名单过滤 + 超时控制） |
| **双层记忆** | ✅ | 会话内短期记忆（InMemory）+ 本地 Markdown 长期记忆，支持自动摘要 |
| **对话分支** | ✅ | 从任意消息分叉出平行对话分支，随时切换 |
| **Agent 工作台** | ✅ | 思考/工具调用步骤以时间线形式可视化 |
| **多模型接入与路由** | ✅ | DeepSeek / 豆包 / OrcaRouter 网关（可选），按问题类型自动路由模型（数学/编程/创作） |
| **知识库 RAG** | ✅ 基础版 | DashScope 向量检索增强回答（内存向量库，产品化进行中） |
| **用户体系** | ✅ | 注册/登录、JWT 鉴权、BCrypt 密码、邮件验证码 |
| **系统通知** | ✅ | WebSocket (STOMP) 实时推送，管理端可创建/撤回 |
| **文件上传** | ✅ | MinIO 对象存储，对话内上传/下载 |

## 技术架构

```
┌────────────────────────────────────────────────────────────────┐
│                         前端层 (Vue 3)                          │
│   对话界面 · Agent 工作台 · 记忆面板 · 分支管理 · 管理后台        │
│              AG-UI 事件流渲染 (fetch + ReadableStream)           │
└────────────────────────────┬───────────────────────────────────┘
                             │ AG-UI (SSE) / REST / WebSocket
┌────────────────────────────▼───────────────────────────────────┐
│                    接入层 (kobeai-api)                          │
│   JWT 鉴权 · Spring Security · 全局异常处理 · Knife4j 文档      │
└────────────────────────────┬───────────────────────────────────┘
                             │
┌────────────────────────────▼───────────────────────────────────┐
│                 智能体层 (kobeai-agent)                         │
│   ┌────────────────┐  ┌─────────────┐  ┌──────────────────┐    │
│   │ ReActAgent     │  │ ModelRouter │  │ Toolkit          │    │
│   │ (AgentScope)   │  │ 按任务路由   │  │ 沙盒/知识库/时间/ │    │
│   │ AG-UI Adapter  │  │             │  │ 记忆/系统 工具    │    │
│   └────────────────┘  └─────────────┘  └──────────────────┘    │
│   短期记忆 InMemory · 长期记忆 LocalFile · MemorySummarizer     │
└────────────────────────────┬───────────────────────────────────┘
                             │
┌────────────────────────────▼───────────────────────────────────┐
│                      业务服务层 (kobeai-service)                │
│   会话/消息 · 用户/鉴权 · 邮件(RabbitMQ 异步) · 文件(MinIO)     │
│   提示词模板优化 · 会话压缩策略（关键词/语义）                   │
└────────────────────────────┬───────────────────────────────────┘
                             │
┌────────────────────────────▼───────────────────────────────────┐
│         持久层：MySQL (JPA) · Redis · RabbitMQ · MinIO          │
└────────────────────────────────────────────────────────────────┘
```

## 快速开始

### 环境要求

- JDK 17+ · Node.js 18+ · pnpm 10+
- MySQL 8.0+ · Redis 6.0+ · RabbitMQ 3.0+
- MinIO（文件上传，可选）

### 后端启动

```bash
cd kobeai_backend

# 1. 准备配置：复制模板并填入你自己的数据库/Redis/API Key 等配置
cp kobeai-api/src/main/resources/application-example.yml kobeai-api/src/main/resources/application.yml

# 2. 初始化数据库（可选，JPA 也会自动建表）
mysql -u root -p < sql/create.sql

# 3. 启动
mvn spring-boot:run -pl kobeai-api
```

> ⚠️ `application.yml` 包含密钥等敏感信息，已被 gitignore，**不要提交到仓库**。

### 前端启动

```bash
cd kobeai_frontend
pnpm install
pnpm dev          # 开发模式，访问 http://localhost:5173
pnpm build        # 生产构建
pnpm lint         # ESLint 检查
pnpm typecheck    # TypeScript 类型检查
```

### 可选 Provider：OrcaRouter

除了直连 DeepSeek / 豆包，项目也支持通过 [OrcaRouter](https://www.orcarouter.ai/ref/ref_bdd09e1fa977b44ced76) 接入模型 —— 一个 OpenAI 兼容的 LLM API 网关，提供自适应路由、自动故障转移，并按供应商原价计费（零加价）。

在 `application.yml` 中配置（不配置则不启用）：

```yaml
app:
  ai:
    orcarouter:
      base-url: https://api.orcarouter.ai
      api-key:${AI_API_KEY}# 在 OrcaRouter 注册后获取
```

配置后 Agent 的可用模型会新增 **`orcarouter-auto`**（对应网关的 `orcarouter/auto` 自适应路由模型，由网关按提示词自动选择最合适的底层模型）。

## 项目结构

```
IntelliFlow-AI/
├── kobeai_backend/                  # Spring Boot 3.5 多模块后端
│   ├── kobeai-common/               # 通用层：DTO、错误处理、常量
│   ├── kobeai-domain/               # 领域层：JPA 实体、Repository
│   ├── kobeai-service/              # 业务层：会话/用户/邮件/文件/模板优化
│   ├── kobeai-agent/                # 智能体层：AgentScope ReActAgent、
│   │                                #   工具(沙盒/知识库/记忆)、模型路由、
│   │                                #   AG-UI 适配、长期记忆与摘要
│   ├── kobeai-api/                  # 接入层：Controller、Security、WebSocket
│   └── sql/                         # 数据库初始化脚本
└── kobeai_frontend/                 # Vue 3 + TypeScript 前端
    └── src/
        ├── views/chat/              # 对话页（布局编排）
        ├── components/chat/         # 侧栏/消息列表/输入框/欢迎页组件
        ├── components/              # AgentWorkbench、MarkdownRenderer 等
        ├── stores/                  # Pinia（对话流、分支、鉴权）
        ├── api/                     # 接口封装与 AG-UI 事件流解析
        └── router/                  # 路由与守卫
```

## 核心设计

### Agent 会话管理

每个会话维护独立的 ReActAgent 实例（含短期记忆），并带有**空闲自动回收**机制：超过 `agent.session.idle-ttl-minutes`（默认 30 分钟）未活跃的会话会被定时任务回收，正在流式输出的会话不会被中断。

### 分页与增量加载

- 会话列表：`GET /chat/conversations?page=&size=` 分页返回，前端支持"加载更多"；
- 消息列表：游标（cursor）分页，前端"加载更早的消息"向上增量加载。

### 长期记忆

Agent 的长期记忆以本地 Markdown 文件持久化在 `data/memory/`（已被 gitignore），`MemorySummarizer` 会定期把会话文件摘要归档，供后续会话参考。

## 技术栈

**后端**
- Spring Boot **3.5.11** · Spring Security · Spring Data JPA
- [AgentScope Java](https://java.agentscope.io) **1.0.12**（ReActAgent / Toolkit / AG-UI Adapter）
- LangChain4j · DeepSeek API · 豆包 API · DashScope Embedding
- [OrcaRouter](https://www.orcarouter.ai/ref/ref_bdd09e1fa977b44ced76) · 可选 LLM 网关 Provider（OpenAI 兼容）
- MySQL 8.0 · Redis · RabbitMQ · MinIO

**前端**
- Vue **3.4** (Composition API) · TypeScript · Vite 5
- Pinia · Element Plus · [markstream-vue](https://www.npmjs.com/package/markstream-vue)（流式 Markdown 渲染）
- ESLint 9 (扁平配置) · Prettier · vue-tsc 2
- AG-UI 事件流：`fetch` + `ReadableStream` + `AbortController`（支持停止生成）

## 测试

```bash
cd kobeai_backend
mvn test           # 运行全部单元测试（模板服务 / 模型路由 / 会话回收）
```

## 路线图

- [x] 基础对话（SSE 流式 + Markdown 渲染）
- [x] 用户体系与 JWT 鉴权
- [x] 集成 AgentScope 实现 ReAct 智能体 + AG-UI 协议
- [x] 代码沙盒工具（Python / JavaScript / Shell）
- [x] 双层记忆系统（短期 + 长期本地文件 + 自动摘要）
- [x] 对话分支、Agent 工作台
- [ ] 知识库 RAG 产品化（向量库持久化 + 文档上传入库 + 知识库管理界面）
- [ ] 代码沙盒容器化隔离（Docker 一次性容器）
- [ ] 管理接口权限细化（/admin/**、/platform/** 角色校验）
- [ ] 模型路由升级（意图分类 + token 成本统计）
- [ ] 可观测性（Actuator + Prometheus 工具调用链路追踪）
- [ ] 插件系统 / MCP 协议支持
- [ ] 多租户能力

## 许可证

[MIT License](LICENSE)

---

<div align="center">
  <p>这是一个持续完善中的学习/演示项目，欢迎 Issue 与 PR。</p>
  <p>如果你觉得有帮助，欢迎点亮 ⭐️ 给作者鼓励！</p>
  <p>Made with ❤️ by <a href="https://github.com/kobewl">@kobewl</a></p>
</div>
