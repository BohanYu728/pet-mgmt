# 宠物管家视觉升级实施计划

> 执行采用 subagent-driven-development：摄影素材与独立页面分别实施，主任务完成全局样式、其余页面及验收。

目标：将现有 Vue 全系统改为用户确认的清爽宠物生活风，加入实际照片，保持所有业务流程。

架构：保留现有 API、路由和 Vue 状态逻辑。共享 Icon 与 Photo 组件统一图标、图片失败处理；全局 CSS 负责一致的布局、交互及移动端。照片作为 public/images 本地资源。

技术：Vue 3、Vite、现有 Axios 与 Leaflet，不添加 UI 框架。

## 全局约束

- 沿用已有接口、权限及业务内容，保留用户上传照片。
- 背景 #F4F7FB、面板 #FFFFFF、正文 #253247、主操作 #E87832（按钮使用更深色保证文字对比）、辅助 #2A7E86、浅色分区 #EAF3F8。
- 首页和登录使用实拍摄影，商品示意图片标明用途，缺图时不产生破图。
- 360px 起无横向溢出，表格可在内部滚动，移动菜单、键盘焦点可用，尊重减少动态效果设置。
- 保留上一版综合报告和原始验收截图，新截图保存 docs/evidence/ui-refresh。

## 任务一 全局与素材

- [x] 创建 frontend/src/components/AppIcon.vue，接口 name/size，纯内联 SVG 统一线条。
- [x] 创建 frontend/src/components/AppPhoto.vue，接口 src/fallback/alt，失败时单次切换到本地 fallback；无图时展示图标和文字。
- [x] 整理 frontend/public/images 摄影文件并在 docs/ui-image-sources.md 记录真实来源及用途。
- [x] 重构 App.vue 与 style.css：顶部、分组侧栏、移动遮罩、按钮表单表格弹窗、状态和空页面；登录页独立布局。

## 任务二 首页与账号

- [x] 调整 DashboardView.vue 为照片主视觉、真实统计、快捷入口、宠物档案和近期提醒。
- [x] 调整 AuthView.vue 为照片与表单双栏，保留注册、资料、密码和上传接口及输入标签。
- [x] 局部样式使用 scoped，复用全局变量及 card/btn/field。

## 任务三 全部业务视图

- [x] 修改 PetsView.vue、ShopView.vue：照片卡片、商品筛选、价格、购物车及订单视觉。
- [x] 修改 JournalView.vue、AiView.vue、CommunityView.vue、AdminView.vue、SearchView.vue：统一标题、分区、工具栏、空状态和真实数据展示。
- [x] 图片只在适合区域加入；维护后台以表格可读性为主。

## 任务四 验收

- [x] 运行 npm.cmd test 和 npm.cmd run build，检查构建及现有交互测试。
- [x] 在本机 H2 demo 中运行 scripts/verify-ui-actions.cjs，要求用户与宠物增删改、购物车和订单9项通过。
- [x] 新建 scripts/check-ui-refresh.cjs，遍历用户、管理员、商户路由，检查 JS 错误、缺图、移动溢出和菜单，并保存桌面及手机截图。
- [x] 查看实际截图，修正裁切、拥挤、对比度和响应式问题；代码审查后记录结果。

自审：各页面均列入改造；没有新增后端或业务需求。摄影与独立视图可并行，最终以浏览器实际流程验收。
