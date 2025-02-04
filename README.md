# IntelliFlow AI - 智能对话平台

IntelliFlow AI 是一个基于人工智能的智能对话平台，旨在为用户提供智能化的对话和服务支持。项目采用前后端分离架构，其中后端使用 Spring Boot 构建，前端则基于 Vue 3 + TypeScript + Vite 开发。

---

## 目录

- [项目描述](#项目描述)
- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [安装与部署](#安装与部署)
  - [前端](#前端)
  - [后端](#后端)
- [项目结构](#项目结构)
- [开发指南](#开发指南)
- [贡献指南](#贡献指南)
- [许可证](#许可证)
- [联系方式](#联系方式)
- [致谢](#致谢)

---

## 项目描述

IntelliFlow AI 项目旨在提供一套智能对话系统，支持多种 AI 模型的对话功能以及文件管理、用户认证、主题切换等丰富特性。该系统采用前后端分离架构，易于维护与扩展。

---

## 功能特性

- 🤖 **智能对话**：支持多种 AI 模型的智能对话功能
- 👥 **用户管理**：完整的用户认证和授权系统（基于 JWT）
- 💬 **实时通信**：采用 WebSocket 实现实时消息推送
- 📁 **文件处理**：支持文件上传、存储与管理（基于 MinIO）
- 🌓 **主题切换**：支持明暗主题模式切换
- 🔒 **安全性**：基于 Spring Security 的权限控制和 JWT 验证

---

## 技术栈

### 前端 (intelliflow_frontend)

- Vue 3 + TypeScript
- Vite (构建工具)
- Element Plus UI 框架
- Pinia 状态管理
- Vue Router 路由管理
- Axios 请求处理
- Markdown 渲染与 WebSocket 实时通信

### 后端 (intelliflow_backend)

- Spring Boot (后端框架)
- Spring Security + JWT (认证和授权)
- Spring Data JPA (数据访问)
- WebSocket (实时通信)
- MinIO (对象存储)
- MySQL (数据库)
- Knife4j (API 文档)

---

## 安装与部署

### 前端部署

1. 进入前端目录：
   ```bash
   cd intelliflow_frontend
   ```
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动开发服务器：
   ```bash
   npm run dev
   ```
4. 构建生产版本：
   ```bash
   npm run build
   ```
   将生成的 `dist` 目录部署到 Web 服务器。

### 后端部署

1. 进入后端目录：
   ```bash
   cd intelliflow_backend
   ```
2. 使用 Maven 构建项目：
   ```bash
   mvn clean install
   ```
3. 运行项目：
   ```bash
   mvn spring-boot:run
   ```
   或者打包后运行 jar 包：
   ```bash
   java -jar target/intelliflow-backend.jar
   ```

---

## 项目结构

```
IntelliFlow/
├── intelliflow_backend/    # 后端 Spring Boot 项目
│   ├── pom.xml
│   ├── src/               # Java 源码与资源
│   └── README.md          # 后端说明
├── intelliflow_frontend/   # 前端 Vue3 + TypeScript 项目
│   ├── package.json
│   ├── src/               # 前端源码
│   ├── public/            # 静态资源
│   └── README.md          # 前端说明
└── README.md              # 当前总项目概览
```

---

## 开发指南

- 遵循 [Vue 3 风格指南](https://v3.vuejs.org/style-guide/) 和 [RESTful API 设计规范](https://restfulapi.net/)
- 编写单元测试、集成测试及详细文档以确保代码质量
- 使用 Git 进行版本管理，按分支开发并提交 Pull Request

---

## 贡献指南

1. Fork 本仓库
2. 创建新分支并提交改动：
   ```bash
   git checkout -b feature/your-feature-name
   git commit -am "Add: 新功能说明或 Bug 修复"
   git push origin feature/your-feature-name
   ```
3. 提交 Pull Request 后等待审核

---

## 许可证

本项目采用 [MIT License](LICENSE)。

---

## 联系方式

- **作者**：王梁
- **邮箱**：[wl313018793@gmail.com](mailto:wl313018793@gmail.com)
- **GitHub**：[kobewl](https://github.com/kobewl)

---

## 致谢

感谢所有帮助改善本项目的开发者！
