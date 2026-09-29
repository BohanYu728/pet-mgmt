"""Preserve original business chapters while updating implementation details."""
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
name = '宠物管家系统项目报告.md'
text = subprocess.check_output(['git','show',f'main:{name}'], cwd=root).decode('utf-8')
combined = (root/'docs/宠物管家系统个人技能实训综合报告.md').read_text(encoding='utf-8-sig')

text = re.sub(r'## 2\.4 Thymeleaf 模板引擎\n.*?(?=## 2\.5)', '''## 2.4 Vue 与前后端分离

本次实训将受支持的前端迁移到独立的 Vue 3 工程，使用 Vue Router 管理页面、Axios 调用 JSON 接口、Vite 完成开发代理与构建。宠物、商城和 AI 业务入口继续保留，前后端可分别部署。

旧 Thymeleaf 模板保留为历史参考；原页面路由转向 Vue，不再作为受支持的业务前端。接口配置 CORS 白名单，Cookie JWT 配套 CSRF 保护，前端不在浏览器存储中保存可读令牌。

''', text, flags=re.S)
text = re.sub(r'认证流程如下：.*?(?=\n\n)', '认证流程如下：用户提交凭据及 CSRF 请求头，后端使用 BCrypt 验证密码并通过 HttpOnly Cookie 签发 JWT。后续请求由浏览器携带 Cookie；过滤器校验签名、有效期和撤销状态，再从数据库读取权限。Controller/Service 检查接口权限和对象归属。前端不在 localStorage 或 sessionStorage 保存 JWT。', text)
text = re.sub(r'本系统选用 Spring Boot 3\.4\.5 版本，.*?(?=\n\n)', '本系统在原有 Spring Boot 3.4.5 基础上实施改造，Maven 使用 Java 17 release 编译目标与 Jakarta 命名空间。保留 JPA 业务数据访问，并新增 MyBatis 用户管理，满足本次 Web 基础项目实验。', text)
text = text.replace('前端基于 Thymeleaf 模板引擎与原生 JavaScript 构建响应式界面', '前端基于 Vue 3、Vue Router 与 Axios 构建独立界面')
text = text.replace('the Thymeleaf template engine and vanilla JavaScript', 'Vue 3, Vue Router and Axios')
text = text.replace('Thymeleaf 模板引擎', 'Vue 前端框架').replace('Thymeleaf 等技术栈', 'Vue 等技术栈')
text = text.replace('前后端一体化的 B/S', '前后端分离的 B/S')
text = text.replace('[Thymeleaf 模板引擎](#thymeleaf-模板引擎)', '[Vue 与前后端分离](#24-vue-与前后端分离)')
text = re.sub(r'\*\*表示层\*\*.*?(?=\n\n)', '**表示层**由独立 Vue 工程提供，沿用宠物管理、照护、医院、商城和 AI 服务的业务入口。Vue Router 管理路由，Axios 调用 REST API，Vite 支持独立构建。旧模板作为参考保留，不再承担受支持的业务交互。', text)
text = text.replace('## 2.2 Spring Data JPA 与 MySQL', '## 2.2 MyBatis Spring Data JPA 与 MySQL\n\n新增用户管理通过 MyBatis Mapper 执行参数化 SQL，Service 负责事务和密码哈希。其余领域模型保留 JPA 映射；两者使用同一数据源，避免重复维护两套业务用户。')
text = text.replace('JPA[Spring Data JPA / Hibernate]', 'JPA[MyBatis 用户 CRUD 与 JPA 业务持久层]')
text = text.replace('Thymeleaf[Vue 前端框架]', 'Thymeleaf[Vue 3 独立前端]')
text = text.replace('（3）基于 Spring Boot 3.4.5 + Java 21 技术栈完成了系统的编码实现，项目包含 70+ 个 Java 源文件、39 个 Thymeleaf 模板页面，代码结构清晰，遵循分层架构和面向对象设计原则。', '（3）基于 Spring Boot 3.4.5、Java 17 编译目标和 Vue 3 完成本次实训改造，新增 MyBatis 用户管理、角色权限表、Cookie JWT、CI 配置和 RAG 助手。验证范围与限制见第 6 章及综合实验报告。')
text = text.replace('经过功能测试与验证，系统各模块运行稳定，满足设计预期。实际使用结果表明，本系统能够有效降低宠物日常管理的复杂度，为宠物主人提供科学化、智能化的养宠决策支持。', '本次改造通过自动测试、接口和页面联调记录验证结果。系统继续提供宠物日常管理与辅助信息；未执行的真实模型调用、远程协作和 MySQL 迁移不记为通过，AI 内容仍需人工核查。')
text = text.replace('After functional testing and verification, all system modules operate stably and meet design expectations. Practical usage results demonstrate that the system effectively reduces the complexity of daily pet management and provides pet owners with scientific and intelligent decision support for pet care.', 'The revised implementation is validated using automated tests and local integration checks. Live model calls, remote collaboration and migration against an existing MySQL database require separate evidence. AI-generated content still needs human verification.')
lab_content = combined.split('## 实验一 ',1)[1].split('## 综合验证记录',1)[0]
lab_content = '## 实验一 ' + lab_content
lab_content = lab_content.replace('](evidence/', '](docs/evidence/')
text = re.sub(r'# 第 5 章 系统实现\n.*?(?=# 第 6 章 系统测试)', '# 第 5 章 系统实现\n\n本章按第二部分个人技能实训组织，五个实验合并交付一份综合报告。原业务背景、需求和数据模型继续保留。\n\n' + lab_content + '\n', text, flags=re.S)
verification = combined.split('## 综合验证记录',1)[1].split('## 总结与反思',1)[0]
text = re.sub(r'# 第 6 章 系统测试\n.*?(?=# 第 7 章 总结与展望)', '# 第 6 章 系统测试\n' + verification + '\n', text, flags=re.S)
text = text.replace('客户端保存Token', '浏览器保存HttpOnly Cookie').replace('localStorage.setItem("token", token)', '保存HttpOnly Cookie')
text = text.replace('JWT 无状态认证', 'JWT 认证与服务端撤销校验')
text = text.replace('   - 5.1 [开发环境搭建](#开发环境搭建)\n   - 5.2 [核心功能实现](#核心功能实现)', '   - 实验一至实验五的实现、验证和 AI 使用记录')
text = text.replace('   - 6.1 [测试环境](#测试环境)\n   - 6.2 [功能测试](#功能测试)\n   - 6.3 [本章小结](#本章小结)', '   - 综合验证记录与未验证条件')
(root/name).write_text(text, encoding='utf-8')
print('Updated project report; original retained in Git main and original Word file.')
