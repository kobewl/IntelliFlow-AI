# IntelliFlow AI — 高性能智能对话平台

<div align="center">

  <img src="https://img.shields.io/badge/Vue-3.4-42b883?logo=vuedotjs" alt="Vue 3.4"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2-6db33f?logo=springboot" alt="Spring Boot 3.2"/>
  <img src="https://img.shields.io/badge/DeepSeek-API-0078d4?logo=openai" alt="DeepSeek"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License"/>
  <img src="https://img.shields.io/github/stars/kobewl/IntelliFlow-AI?style=social" alt="Stars"/>

  <h3>多模型调度 · 知识库增强 · 多轮记忆</h3>
  <p>单机日均可处理对话量 <strong>10W+</strong> 的企业级 AI 对话引擎</p>
</div>

---

## 项目亮点

- **多模型混合调度** — 智能路由 DeepSeek、文心一言等主流大模型，根据任务类型自动选择最优模型
- **知识库增强 (RAG)** — 基于向量数据库实现私有知识库检索，让 AI 回答更精准、更专业
- **多轮对话记忆** — 完整的上下文管理，支持长对话链路的记忆保持与召回
- **高并发架构** — 基于 Spring Boot 3 + 异步线程池，单机支撑 10W+ 日对话量
- **响应式前端** — Vue 3 Composition API + TypeScript，流畅的流式输出体验

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 3)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 对话界面      │  │ 知识库管理    │  │ 模型配置      │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└────────────────────────────┬────────────────────────────────┘
                             │ WebSocket / SSE
┌────────────────────────────▼────────────────────────────────┐
│                    网关层 (Spring Boot)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 认证/鉴权     │  │ 限流/熔断     │  │ 请求路由      │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      核心业务层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 对话引擎      │  │ 模型调度器    │  │ 记忆管理器    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ RAG 检索      │  │ 意图识别      │  │ 会话管理      │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      基础设施层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 向量数据库    │  │ 关系数据库    │  │ Redis 缓存    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

## 功能演示

| 功能 | 描述 |
|------|------|
| **智能对话** | 流式输出、Markdown 渲染、代码高亮 |
| **模型切换** | 一键切换 DeepSeek / 文心一言 / 自定义模型 |
| **知识库问答** | 上传文档 → 自动切片 → 向量化 → 精准问答 |
| **对话历史** | 完整的会话管理，支持搜索与导出 |
| **系统配置** | 温度、Max Tokens、系统提示词等参数可调 |

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

### 后端启动

```bash
cd kobeai_backend
# 配置 application.yml 中的数据库和模型 API Key
mvn spring-boot:run
```

### 前端启动

```bash
cd kobeai_frontend
npm install
npm run dev
```

访问 http://localhost:5173

## 项目结构

```
IntelliFlow-AI/
├── kobeai_backend/           # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/kobeai/
│   │       ├── controller/   # REST API 接口
│   │       ├── service/      # 业务逻辑层
│   │       ├── domain/       # 领域模型
│   │       ├── infrastructure/# 基础设施（模型客户端、向量存储）
│   │       └── config/       # 配置类
│   └── src/main/resources/
│       └── application.yml
└── kobeai_frontend/          # Vue 3 前端
    ├── src/
    │   ├── views/            # 页面组件
    │   ├── components/       # 通用组件
    │   ├── stores/           # Pinia 状态管理
    │   ├── api/              # 接口请求封装
    │   └── utils/            # 工具函数
    └── vite.config.ts
```

## 核心设计

### 模型调度策略

```java
// 根据任务类型智能路由模型
ModelRouter router = ModelRouter.builder()
    .when(TaskType.CODING).use(Model.DEEPSEEK_CODER)
    .when(TaskType.WRITING).use(Model.ERNIE_BOT)
    .when(TaskType.ANALYSIS).use(Model.DEEPSEEK_V3)
    .build();
```

### RAG 检索流程

```
用户提问 → 意图分析 → 向量检索 → 文档重排 → 上下文组装 → 模型生成
```

## 性能指标

| 指标 | 数据 |
|------|------|
| 单机日对话量 | 100,000+ |
| 平均响应延迟 | < 800ms |
| 并发连接数 | 500+ |
| 知识库检索精度 | 92%+ |

## 技术栈

**后端**
- Spring Boot 3.2 · Spring Security · MyBatis-Plus
- DeepSeek API · 文心一言 API
- Milvus / pgvector · MySQL · Redis

**前端**
- Vue 3 · TypeScript · Vite
- Pinia · Element Plus · Marked.js
- WebSocket / SSE 流式输出

## 路线图

- [x] 多模型混合调度
- [x] 知识库 RAG 增强
- [x] 多轮对话记忆
- [ ] 插件系统
- [ ] 多租户 SaaS 化
- [ ] 移动端 App

## 许可证

[MIT License](LICENSE)

---

<div align="center">
  <p>如果这个项目对你有帮助，请点亮 ⭐️ 支持一下！</p>
  <p>Made with ❤️ by <a href="https://github.com/kobewl">@kobewl</a></p>
</div>
