# 宠物管家系统

在原有宠物档案、健康记录、商城、商户管理和 AI 功能上，增加个人技能实训所需的 Vue 前后端分离、MyBatis 用户管理、角色权限表、Cookie JWT、CI 和基于项目资料的 RAG 助手。五个实验按章节合并到一份综合报告。

## 启动演示环境

准备 JDK 17 或兼容版本、Maven、Node.js 22。后端编译目标为 Java 17。

```powershell
mvn.cmd "-Dmaven.repo.local=.m2repo" verify
java -jar target/pet-mgmt-java-1.0.0.jar --spring.profiles.active=demo
```

另开终端进入 frontend：

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

访问 [样例体验中心](http://127.0.0.1:5173/demo)，选择宠物主人、商户或管理员一键进入。九个场景可直接操作，预置宠物、健康记录、提醒、体重、日志、商品、订单和虚构租金账单。所有修改经由正常业务接口，重启 demo 恢复初始样例。也可访问 [Vue 页面](http://127.0.0.1:5173)；后端默认使用 8080 端口。演示账户为普通用户 `demo / demo123`、管理员 `admin / admin123`、商户 `demo_shop / demoShop123`，仅在 demo profile 的内存数据库中初始化；重启后演示数据清空。真实 MySQL 数据不会被演示模式读取或修改。演示商品及医院均为标明用途的虚构数据。

AI 样例：在体验中心的“AI 样例体验”区域选择资料问答、品种识别、情绪分析、健康报告、商品推荐或症状咨询。点击“填入样例”，可编辑问题、症状或替换照片，再提交生成实际结果。样例入口仅在 demo 环境显示；无真实模型凭据时，资料问答显示检索模式，其他功能使用本地算法。

前端开发服务器将 `/api` 和 `/uploads` 转发到后端，认证通过 HttpOnly Cookie 携带 JWT；浏览器 JavaScript 只读取独立的 CSRF token。

## 使用现有 MySQL

先备份数据库，再通过 MySQL 客户端执行 `src/main/resources/db/migration/V1__rbac.sql`。该脚本新增角色权限及关联表，并从 users.role 导入关系，不清空已有业务表。新数据库需要先创建原有业务表；可使用原系统数据库结构，或在受控环境让 JPA 初始化后再执行该脚本。

设置进程环境变量后，不使用 demo profile 启动：

```powershell
$env:DB_USERNAME='你的数据库用户名'
$env:DB_PASSWORD='你的数据库密码'
$env:JWT_SECRET='至少32字节的随机签名密钥'
$env:CORS_ORIGIN='https://你的前端域名'
$env:COOKIE_SECURE='true'
java -jar target/pet-mgmt-java-1.0.0.jar
```

数据库地址可通过 `DB_URL` 或 Spring Boot 的 `SPRING_DATASOURCE_URL` 覆盖。`FRONTEND_URL` 指定旧页面路由跳转的 Vue 地址。`CORS_ORIGIN` 支持逗号分隔的精确来源，默认仅允许 localhost 和 127.0.0.1 的 5173 端口。生产部署使用 HTTPS；部署前将 JPA 的 `ddl-auto` 设为 `validate`，显式审核结构迁移。不要把真实凭据提交到 Git，也不要在生产环境启用 demo profile。

前端独立构建 `npm.cmd run build`，将 `frontend/dist` 部署到静态服务器并配置 history 回退到 index.html；反向代理 `/api`、`/uploads` 到后端。跨域部署通过 `VITE_API_BASE_URL` 和 `CORS_ORIGIN` 配套配置，且使用带凭据请求。

## 配置智能助手

RAG 助手使用独立的后端环境变量：

- `OPENAI_API_KEY`：兼容服务的凭据。
- `OPENAI_BASE_URL`：API 根地址，默认 `https://api.openai.com/v1`。
- `OPENAI_CHAT_MODEL`：对话模型名称。
- `OPENAI_EMBEDDING_MODEL`：向量模型名称。

需要同时支持 chat/completions 和 embeddings 的服务。仅支持聊天的接口不能完成真实向量检索；本地 Ollama 应加载对应聊天和嵌入模型，并配置兼容接口地址及服务要求的占位凭据。

项目资料位于 `src/main/resources/knowledge`。后端切块、向量化、检索并校验引用。没有可用模型、请求失败或引用不合法时，只显示明确标注的资料原文，不宣称模型调用成功。

原有健康报告、症状咨询等功能使用 `LLM_API_KEY`（或原有 `DEEPSEEK_API_KEY`）、`LLM_API_URL`、`LLM_MODEL`；未配置时使用原系统本地算法。这些启发式模型及 AI 回答不替代兽医诊断。

## 验证与文档

```powershell
mvn.cmd "-Dmaven.repo.local=.m2repo" verify
cd frontend
npm.cmd test
npm.cmd run build
```

- OpenAPI：`http://127.0.0.1:8080/swagger-ui/index.html`
- Git 协作与冲突练习：`docs/git-collaboration.md`
- 本地 Git 练习原始记录：`docs/evidence/git-exercise.txt`
- CI：`.github/workflows/ci.yml`
- PR 模板：`.github/pull_request_template.md`
- 最终实验材料：`宠物管家系统个人技能实训综合报告.docx`

报告按实验一至五分章，每章包含步骤、代码、验证、总结反思和 AI 使用记录。远程 PR、云端 CI、真实模型服务及人工答辩只有实际执行后才能记录为完成。

商户租金账单提供只读查询。原项目支付模板未连接真实支付服务，本次也未接入支付网关，不把模拟按钮标为已完成的线上缴费。

## 三种角色的界面范围

宠物主人使用日常照护、医院、商城与 AI；商户使用商品、订单和租金账单；管理员使用平台统计、用户、宠物和商户审核。登录后及点击品牌标识均进入自己的工作台，其他角色页面会跳回当前角色首页。个人资料和退出通用；demo 中保留角色切换，样例场景按当前角色过滤。前端路由分离不替代后端权限校验。
