# 个人技能实训实施计划

> **For agentic workers:** Use subagent-driven-development to implement and review the bounded tasks below.

**Goal:** 保留宠物管家业务，完成五个实验对应代码，交付一份综合实验报告。

**Architecture:** Spring Boot 3 / Java 17 后端复用现有业务，Vue 3 独立前端。MyBatis 承担用户管理，JPA 保留其他业务。Cookie JWT、CSRF、角色权限表与 RAG 配套实现。

**Tech Stack:** Java 17, Maven, Spring Boot, MySQL, MyBatis, Spring Security, Vue 3, Axios, Vite。

## Global Constraints

- 保留已有业务、图片与数据；不清空真实数据库。
- 前端不持久保存可读 JWT，密码只保存 BCrypt 哈希，密钥由环境变量注入。
- 五个实验合并为一份综合报告，分章记录真实验证与 AI 使用情况。
- 不伪造远程 PR、人工评审、模型调用或运行截图。

## Task 1：基础、安全与用户管理

- [ ] 配置 Java 17、MyBatis、OpenAPI 和 H2 测试环境；外置敏感配置。
- [ ] 对未认证、CSRF、角色越权、登录 Cookie 和用户 CRUD 先写失败测试。
- [ ] 实现角色权限表、MyBatis 用户 Mapper/Service/API，保护业务写接口。
- [ ] 执行 Maven 测试，核对用户返回值不包含密码或令牌。

接口：`/api/auth/csrf` 返回 token/headerName；`/api/auth/login` 设置 HttpOnly token Cookie；`/api/auth/profile` 返回 user；`/api/auth/logout` 清除 Cookie。用户管理 `/api/admin/users`。

## Task 2：完整 Vue 前端与业务接口

- [ ] 盘点现有模板、API、实体和缺失的业务接口。
- [ ] 创建 frontend 的 Vue/Vite/Router/Axios 工程及统一错误处理。
- [ ] 迁移用户、宠物管理、健康/体重/提醒/相册/日志、医院、商城、商户、管理员及原有 AI 入口。
- [ ] 补齐缺失的业务 REST Controller，保留对象归属校验。
- [ ] 添加关键交互测试，执行 npm test 与 npm run build。

前端调用后端 `/api`；认证使用 credentials Cookie 与 CSRF 请求头，不保存 JWT。RAG `POST /api/assistant/ask` 请求 `{question}`，响应 `{answer,sources,status}`。

## Task 3：LLM SDK 与 RAG

- [ ] 先测试切块、检索排序、无资料和无模型配置的返回行为。
- [ ] 添加 SDK 客户端、资料切块、embedding、检索、引用校验和输入边界。
- [ ] 实现 `/api/assistant/ask`，模型不可用时明确状态并显示资料。
- [ ] 执行独立测试，实际模型未配置时如实记录限制。

仅修改新 ai/rag 包、AssistantController、knowledge 资料和相应测试；依赖变更协调主代理完成。

## Task 4：集成、CI 和综合报告

- [ ] 建立 Git 基线与 feature/personal-training 分支，排除密钥及运行数据。
- [ ] 增加 PR 模板和后端/前端 CI，并执行对应本地命令。
- [ ] 启动隔离演示环境，浏览器联调、保存截图，审查完整变更。
- [ ] 同步项目 Markdown 报告，生成一份综合 Word 报告；逐实验记录步骤、代码、验证、总结反思与 AI 使用记录。
- [ ] 渲染检查报告，提供运行入口和未验证外部条件清单。
