# IntelliFlow AI 后端

IntelliFlow AI 后端服务基于 Spring Boot 构建，为智能对话平台提供 API、实时通信、用户认证和文件管理等功能。

---

## 技术栈

- **Java 8 或更高**：开发语言
- **Spring Boot 2.7.5**：微服务架构
- **Spring Security + JWT**：认证与授权
- **Spring Data JPA**：数据持久层
- **MySQL 8.0**：关系型数据库
- **WebSocket**：实时消息推送
- **MinIO**：对象存储解决方案
- **Knife4j**：API 文档生成

---

## 快速开始

### 安装与编译

1. 进入项目目录：
   ```bash
   cd intelliflow_backend
   ```
2. 使用 Maven 清理并构建项目：
   ```bash
   mvn clean install
   ```

### 运行项目

启动 Spring Boot 服务：

```bash
mvn spring-boot:run
```

或者打包后运行 jar 包：

```bash
java -jar target/intelliflow-backend.jar
```

---

## 环境配置

1. 复制配置文件：
   ```bash
   cp application-example.yml application.yml
   ```
2. 配置内容详解：
   - **数据库连接**：设置 MySQL 的 URL、用户名及密码
   - **MinIO 配置**：配置对象存储的地址、端口、access key 与 secret key
   - **JWT 密钥**：用于安全认证的密钥
   - **其他服务接口**：如 AI 平台接口密钥等

---

## 项目结构

```
intelliflow_backend/
├── pom.xml                  # Maven 配置文件
├── src/                   # 后端源码和资源
│   ├── main/
│   │   ├── java/          # Java 源代码
│   │   └── resources/     # 配置文件、API 文档，静态资源等
│   └── test/              # 单元测试代码
├── logs/                   # 日志输出目录
└── README.md                # 本文档
```

---

## 开发指南

- 遵循 Spring Boot 和 RESTful API 设计原则
- 编写详细的单元测试与集成测试，保证服务稳定性
- 使用 Git 管理版本，按功能分支进行开发
- 生成 API 文档（建议使用 Knife4j），方便前端对接

---

## 贡献指南

1. Fork 本仓库
2. 创建新分支：
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. 提交改动并推送：
   ```bash
   git commit -am "Update: 新增描述或修复 Bug"
   git push origin feature/your-feature-name
   ```
4. 创建 Pull Request，与团队协作审核

---

## 许可证

本项目后端部分采用 [MIT License](../LICENSE)。

---

## 联系方式

- **作者**：KobeWang
- **邮箱**：[your-email@example.com](mailto:your-email@example.com)
- **GitHub**：[kobewl](https://github.com/kobewl)

---

## 致谢

感谢所有对 IntelliFlow AI 后端做出贡献的开发者与开源社区的支持！
