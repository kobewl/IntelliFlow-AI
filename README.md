# KobeAI - 智能校园助手

KobeAI 是一个基于人工智能的校园助手系统，旨在为学生和教师提供智能化的学习和工作支持。

## 项目结构

项目采用前后端分离架构：

- `kobeai_frontend/`: Vue3 + TypeScript 开发的前端项目
- `kobeai_backend/`: Spring Boot 开发的后端项目

## 技术栈

### 前端 (kobeai_frontend)

- Vue 3 + TypeScript
- Vite
- Element Plus
- Pinia 状态管理
- Vue Router
- Axios HTTP 客户端
- Markdown 渲染支持
- WebSocket 实时通信

### 后端 (kobeai_backend)

- Spring Boot
- Spring Security + JWT 认证
- Spring Data JPA
- WebSocket
- MinIO 对象存储
- MySQL 数据库
- Knife4j API 文档

## 功能特性

- 🤖 智能对话：支持多种 AI 模型的对话功能
- 👥 用户管理：完整的用户认证和授权系统
- 💬 实时通信：基于 WebSocket 的实时消息推送
- 📁 文件处理：支持文件上传和管理
- 🌓 主题切换：支持明暗主题模式
- 🔒 安全性：JWT 认证和权限控制

## 快速开始

### 前端启动

```bash
cd kobeai_frontend
# 安装依赖
npm install
# 启动开发服务器
npm run dev
```

### 后端启动

```bash
cd kobeai_backend
# 编译项目
mvn clean install
# 运行项目
mvn spring-boot:run
```

## 环境要求

- Node.js >= 16
- JDK >= 17
- Maven >= 3.6
- MySQL >= 8.0

## 配置说明

### 前端配置

1. 复制 `.env.example` 到 `.env`
2. 根据需要修改环境变量

### 后端配置

1. 复制 `application-example.yml` 到 `application.yml`
2. 配置数据库连接信息
3. 配置 MinIO 存储信息
4. 配置 AI 平台接口密钥

## 部署说明

### 前端部署

```bash
# 构建生产版本
npm run build
# 部署 dist 目录到 Web 服务器
```

### 后端部署

```bash
# 打包
mvn clean package
# 运行 jar 包
java -jar target/kobeai-backend.jar
```

## 开发指南

- 遵循 [Vue 3 风格指南](https://v3.vuejs.org/style-guide/)
- 使用 TypeScript 编写前端代码
- 遵循 RESTful API 设计规范
- 编写单元测试和文档

## 贡献指南

1. Fork 本仓库
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 许可证

[MIT License](LICENSE)

## 联系方式

- 作者：KobeWang
- 邮箱：[your-email@example.com]
- GitHub：[@kobewl](https://github.com/kobewl)

## 致谢

感谢所有为本项目做出贡献的开发者！
