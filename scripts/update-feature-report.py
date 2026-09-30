"""Append actual feature screenshots and bounded verification findings to the report."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
data = json.loads((root / 'docs/evidence/features/audit.json').read_text(encoding='utf-8'))
assert data['checks'] and all(item['passed'] for item in data['checks'])
assert len(data['shots']) == 33
source = root / 'docs/宠物管家系统个人技能实训综合报告.md'
text = source.read_text(encoding='utf-8').split('\n## 功能逐项验证与展示')[0].rstrip()
text = text.replace('mvn.cmd -Dmaven.repo.local=.m2repo verify', 'mvn.cmd "-Dmaven.repo.local=.m2repo" verify')
if '| 全功能补充实测 |' not in text:
    text = text.replace('| 本地 Git 练习 |', '| 全功能补充实测 | 109 项检查通过，33 个展示场景 | 独立 H2 中进行接口写入、浏览器提交、结果展示与删除验证；不代表 AI 准确率 |\n| 本地 Git 练习 |')
chapter = '''

## 功能逐项验证与展示

本轮在独立 H2 演示服务中完成 109 项功能与展示检查，保存 33 个实际界面场景；另行重新执行后端测试，34 项通过，0 失败、0 错误。演示过程不会读取或修改真实 MySQL 数据。下方按功能给出入口、实际操作、截图及限制，原始结果保存在 docs/evidence/features/audit.json。

结论：本次覆盖的基础管理、照护、商城及后台操作流程通过；不能笼统宣称所有功能已达到生产可用。AI 页面能提交和返回内容，不等于输出准确；外部服务、真实数据迁移和线上支付需要单独验证。

| 功能类别 | 当前结果 | 验证边界 |
| --- | --- | --- |
| 账号与日常管理 | 本次流程检查通过 | 注册登录、资料密码、头像、宠物、照护记录、相册、搜索 |
| 医院与商城 | 本次流程检查通过 | 测试坐标、演示医院、评价、商品筛选、购物车、下单及取消 |
| 管理员与商户 | 本次流程检查通过 | 用户与宠物管理、入驻审核、商品维护及订单状态流转 |
| 品种与情绪识别 | 原型流程可用，算法能力受限 | 品种为颜色启发式猫分类，固定 75% 不是测得准确率；情绪来自图像特征规则 |
| 健康报告 推荐 咨询 | 本地结果返回正常 | 本地模型或规则仅供参考，未验证真实外部模型；健康评分模型使用合成训练数据 |
| RAG 助手 | 资料检索与引用展示通过 | 无真实模型凭据时展示 retrieval_only，不能标记为远程推理成功 |
| 账单与支付 | 仅账单查询 | 当前无演示账单，未接入支付网关；商户修改已付款状态不代表真实收款 |

展示说明：照片、账号、收货信息、商品、医院和评价仅用于本地功能演示。截图覆盖主要入口及操作结果；同一页面中的新增、编辑、删除等操作合并说明，其实际请求结果保留在检查记录中。
'''
for index, shot in enumerate(data['shots'], 1):
    title = shot['title']
    chapter += f'\n### 功能 {index:02d} {title}\n\n入口：`{shot["route"]}`。{shot["note"]}\n\n![图{index + 9} {title}实际展示](evidence/features/{shot["id"]}.png)\n'
source.write_text(text + chapter, encoding='utf-8')
print(f'Updated report with {len(data["shots"])} feature demonstrations')
