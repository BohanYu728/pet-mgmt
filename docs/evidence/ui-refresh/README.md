# 清爽宠物生活风 UI 验收

2026-09-29，本机 Spring Boot H2 demo + Vue Vite，Chrome 无头浏览器。

- 前端单元测试：6/6；生产构建通过。
- 页面检查：67/67，覆盖登录注册、22 个用户路由、4 个管理员路由、3 个商户路由，桌面 1440px 和手机 360/390px；检查横向溢出、图片失败、页面错误、菜单开关与助手来源展示。
- 操作回归：9/9，覆盖管理员用户增删改、宠物增删改、加入购物车、下单和取消订单。
- 人工查看登录、首页、宠物、商城、健康记录、医院、助手、管理员与商户截图。
- 图片来自本地静态资源，来源与授权见 ../../ui-image-sources.md；默认宠物照片和商品分类照片仅作示意。

结果文件：verification.json、ui-actions-verification.json。截图均在本目录，上一版综合报告和原始截图保持不变。

运行命令（需安装 Playwright 和 Chrome）：

```powershell
$env:NODE_PATH='C:/Users/ybh/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules'
node scripts/check-ui-refresh.cjs
$env:UI_EVIDENCE_DIR='docs/evidence/ui-refresh'
node scripts/verify-ui-actions.cjs
```

本次验证限本机演示环境，助手以资料检索兜底展示来源；未验证外部模型服务或生产部署。

代码复查：符合设计范围，发现的替代照片标识问题已修复；图片失败与恢复的 4 项浏览器检查通过，见 photo-fallback-review.json。
