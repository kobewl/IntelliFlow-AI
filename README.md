# IntelliFlow AI — 智能对话平台

<div align="center">

  <img src="https://img.shields.io/badge/Vue-3.4-42b883?logo=vuedotjs" alt="Vue 3.4"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=springboot" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Java-17-orange?logo=openjdk" alt="Java 17"/>
  <img src="https://img.shields.io/badge/DeepSeek-API-0078d4?logo=openai" alt="DeepSeek"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License"/>
  <img src="https://img.shields.io/github/stars/kobewl/IntelliFlow-AI?style=social" alt="Stars"/>

  <h3>多模型接入 · 流式对话 · 全栈开箱即用</h3>
  <p>一个面向学习与中小团队场景的全栈 AI 对话平台</p>
</div>

---

## 项目亮点

- **多模型接入** — 已集成 DeepSeek、豆包(DouBao)等大模型 API,统一的 Provider 抽象方便后续扩展
- **流式对话** — SSE 流式输出,Markdown / 代码高亮渲染,体验接近 ChatGPT
- **全栈开箱即用** — 前后端分离架构,自带用户体系、JWT 鉴权、对话历史、文件上传(MinIO)
- **现代前端** — Vue 3 Composition API + TypeScript + Vite,组件化清晰
- **可扩展架构** — Service 层抽象 + Repository 模式,易于二次开发

> 💡 部分高级特性(多模型智能路由、知识库 RAG、向量检索)处于**规划/试验阶段**,详见下方[路线图](#路线图)。

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 3)                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 对话界面      │  │ 历史会话      │  │ 系统设置      │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└────────────────────────────┬────────────────────────────────┘
                             │ SSE / REST
┌────────────────────────────▼────────────────────────────────┐
│                  接入层 (Spring Boot 3.5)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ Spring       │  │ JWT 鉴权      │  │ 全局异常处理   │       │
│  │ Security     │  │              │  │              │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      业务服务层                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ ChatService  │  │ User/Auth    │  │ FileService  │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │ DeepSeek     │  │ DouBao       │   ... (Provider 抽象)   │
│  │ Service      │  │ Service      │                         │
│  └──────────────┘  └──────────────┘                         │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│                      持久层                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ MySQL (JPA)  │  │ Redis 缓存    │  │ MinIO 文件    │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

## 功能演示

| 功能 | 状态 | 描述 |
|------|------|------|
| **智能对话** | ✅ | SSE 流式输出、Markdown 渲染、代码高亮 |
| **模型切换** | ✅ | DeepSeek / 豆包,后端 Provider 模式抽象 |
| **用户体系** | ✅ | 注册/登录、JWT、邮件验证 |
| **对话历史** | ✅ | 会话保存、历史检索 |
| **文件上传** | ✅ | MinIO 对象存储 |
| **知识库 RAG** | 🚧 规划 | 计划基于 LangChain4j + 向量库实现 |
| **多模型智能路由** | 🚧 规划 | 按任务类型自动调度模型 |

## 快速开始

### 环境要求

- **JDK 1.8+**
- **Node.js 18+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **MinIO**(用于文件上传,可选)

### 后端启动

```bash
cd kobeai_backend
# 在 application.yml 中配置:
# - 数据库连接 (spring.datasource)
# - Redis 地址
# - DeepSeek / 豆包 API Key
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
├── kobeai_backend/           # Spring Boot 3.5 后端
│   ├── src/main/java/
│   │   └── com/kobeai/hub/
│   │       ├── controller/   # REST API 接口
│   │       ├── service/      # 业务逻辑层
│   │       │   ├── impl/     # 实现类(ChatServiceImpl、各 Provider)
│   │       │   └── AI/       # 各模型 Provider 抽象
│   │       ├── model/        # JPA 实体
│   │       ├── repository/   # 数据访问层 (Spring Data JPA)
│   │       ├── config/       # Spring 配置
│   │       └── utils/        # 工具类
│   └── src/main/resources/
│       └── application.yml
└── kobeai_frontend/          # Vue 3 前端
    ├── src/
    │   ├── views/            # 页面
    │   ├── components/       # 通用组件
    │   ├── stores/           # Pinia
    │   ├── api/              # 接口封装
    │   └── utils/            # 工具函数
    └── vite.config.ts
```

## 核心设计

### Provider 模式抽象多模型

后端通过 `AIPlatformService` 接口统一抽象不同大模型 Provider,新增模型只需实现接口:

```java
public interface AIPlatformService {
    void initialize();
    void close();
    SseEmitter sendMessage(String message);
}
```

目前已实现:`DeepSeekServiceImpl`、`DouBaoServiceImpl`。

### 流式输出

后端使用 `SseEmitter` + 异步线程池,前端使用 `EventSource` 接收增量内容,实现打字机效果。

## 技术栈

**后端**
- Spring Boot **3.5.11** · Spring Security · Spring Data JPA
- LangChain4j **1.0.0-beta3** (大模型集成)
- DeepSeek API · 豆包 API
- MySQL **8.0** · Redis · RabbitMQ · MinIO

**前端**
- Vue **3.4** · TypeScript · Vite
- Pinia · Element Plus · Marked.js
- SSE 流式输出

## 路线图

- [x] 基础对话(SSE 流式输出 + Markdown 渲染)
- [x] 多模型 Provider 抽象(DeepSeek、豆包)
- [x] 用户体系与 JWT 鉴权
- [x] 对话历史持久化
- [ ] 知识库 RAG (基于 LangChain4j + 向量库)
- [ ] 多模型智能路由(按任务类型选择最优模型)
- [x] backend 升级至 JDK 17 + Spring Boot 3.x
- [ ] 集成 [AgentScope](https://github.com/agentscope-ai/agentscope-java) 实现智能体编排
- [ ] 插件系统 / MCP 协议支持
- [ ] 多租户能力

## 许可证

[MIT License](LICENSE)

---

<div align="center">
  <p>这是一个持续完善中的学习/演示项目,部分高级功能仍在规划中。</p>
  <p>如果你觉得有帮助,欢迎点亮 ⭐️ 给作者鼓励!</p>
  <p>Made with ❤️ by <a href="https://github.com/kobewl">@kobewl</a></p>
</div>
