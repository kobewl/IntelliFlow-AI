# KobeAI 前端

这是 KobeAI 项目的前端部分，使用 Vue 3 + TypeScript 以及 Vite 构建。项目提供了智能对话界面、实时通信与丰富的 UI 交互。

---

## 技术栈

- **Vue 3** & **TypeScript**: 主流前端框架与语言
- **Vite**: 快速的开发与构建工具
- **Element Plus**: 现代化 UI 组件库
- **Pinia**: 状态管理库
- **Vue Router**: 路由管理
- **Axios**: HTTP 请求处理
- **Markdown 渲染**: 支持 Markdown 格式内容显示
- **WebSocket**: 实时通信支持

---

## 快速开始

### 安装依赖

1. 进入项目目录：
   ```bash
   cd kobeai_frontend
   ```
2. 安装所有依赖：
   ```bash
   npm install
   ```

### 开发模式

启动开发服务器：

```bash
npm run dev
```

服务器启动后，访问 [http://localhost:3000](http://localhost:3000) 以预览应用。

### 构建生产版本

生成生产环境下的静态资源：

```bash
npm run build
```

构建完成后，请将生成的 `dist` 目录部署至 Web 服务器。

---

## 环境配置

1. 复制 `.env.example` 文件为 `.env`：
   ```bash
   cp .env.example .env
   ```
2. 根据需求配置环境变量，比如 API 地址、WebSocket 端口等。

---

## 项目结构

```
kobeai_frontend/
├── package.json               # 项目依赖与脚本
├── vite.config.ts             # vite 配置
├── src/                      # 项目源码目录
│   ├── components/          # 组件库
│   ├── views/               # 页面视图
│   ├── router/              # 路由配置
│   ├── store/               # Pinia 状态管理（或 Vuex）
│   └── api/                 # API 请求封装
├── public/                    # 静态资源
└── README.md                  # 本文档
```

---

## 开发指南

- 遵循 [Vue 3 风格指南](https://v3.vuejs.org/style-guide/)
- 使用 TypeScript 提高代码稳定性和可维护性
- 编写清晰的注释与文档，确保团队协作顺畅

---

## 贡献指南

1. Fork 本仓库
2. 创建新分支：
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. 提交代码并推送：
   ```bash
   git commit -am "Feature: 描述新增功能或修复的 Bug"
   git push origin feature/your-feature-name
   ```
4. 创建 Pull Request，与团队讨论并合并代码

---

## 许可证

本项目前端部分采用 [MIT License](../LICENSE)。

---

## 联系方式

- **作者**：KobeWang
- **GitHub**：[kobewl](https://github.com/kobewl)
- **邮件**：[your-email@example.com](mailto:your-email@example.com)
