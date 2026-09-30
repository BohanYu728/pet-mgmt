"""Integrate verified demonstrations into their experiment chapters; safe to rerun."""
import json
import re
from pathlib import Path
root = Path(__file__).resolve().parents[1]
source = root / 'docs/宠物管家系统个人技能实训综合报告.md'
data = json.loads((root / 'docs/evidence/features/audit.json').read_text(encoding='utf-8'))
text = source.read_text(encoding='utf-8').split('\n## 功能逐项验证与展示')[0].rstrip()
text = re.sub(r'\n<!-- demos:start -->.*?<!-- demos:end -->\n', '\n', text, flags=re.S)
# Keep technical evidence and mobile layout, replace duplicate overview screenshots.
text = re.sub(r'!\[[^]]*\]\(evidence/ui-refresh/(users-admin|dashboard-desktop|health-desktop|shop-desktop|login-desktop|ai-assistant-desktop|assistant-desktop|assistant-answer)\.png\)\n*', '', text)
mapping = {1:[4,25,26,27], 2:[5,6,7,8,9,10,11,12,13,14,15,16,17,18,30,31,32], 3:[], 4:[1,2,3,28,29,33], 5:[19,20,21,22,23,24]}
parts = re.split(r'(?=\n## )', text)
feature_number = 0
for i, part in enumerate(parts):
    match = re.match(r'\n## 实验([一二三四五])', part)
    if not match: continue
    lab = '一二三四五'.index(match[1]) + 1
    if not mapping[lab]: continue
    block = '\n<!-- demos:start -->\n### 功能展示\n\n以下展示来自本地 H2 演示环境的实际操作，入口和验证范围随图说明。\n'
    if lab == 5:
        block += '\nAI 样例使用步骤：在“样例体验”的 AI 区域选择功能，以宠物主人登录后点击“填入样例”，核对或修改输入，再点击“发送问题”或“开始分析”。六项功能均调用现有接口生成结果；填入样例不会自动提交，也不会展示预编造的答案。\n'
    for number in mapping[lab]:
        feature_number += 1
        shot = data['shots'][number - 1]
        note, image = shot['note'], f'evidence/features/{shot["id"]}.png'
        if lab == 5:
            section = shot['id'].removeprefix('ai-')
            image = f'evidence/ai-samples/{section}.png'
            inputs = {
                'assistant': '点击“填入样例”载入“如何设置疫苗提醒？”，可改写后发送，查看实际状态与资料来源。',
                'breed': '点击“填入样例”加载猫咪照片并关联咪咪；可上传自己的图片替换，提交后生成识别与历史记录。',
                'emotion': '使用猫咪照片样例并关联咪咪，提交后查看本次情绪分析和历史记录。',
                'health': '点击“填入样例”选择旺财，以现有档案和健康记录生成报告；可改选其他宠物。',
                'recommend': '选择旺财样例，提交后基于宠物资料与在售商品生成推荐。',
                'consultation': '填入狗狗食欲变化的虚构描述与“一天”的持续时间，核对或修改后提交咨询。'
            }
            note = inputs[section] + note
        if number == 32:
            note = '当前内置一笔虚构租金账单，可查询金额与期间；仅供演示，没有实际扣款或在线缴费。'
            image = 'evidence/demo/billing.png'
        block += f'\n#### 功能 {feature_number:02d} {shot["title"]}\n\n入口：`{shot["route"]}`。{note}\n\n![{shot["title"]}实际展示]({image})\n'
    if lab == 2:
        block += '''
#### 样例体验入口

入口：`/demo`，也可以点击页面顶部“样例体验”。选择宠物主人、商户或管理员，会通过普通登录接口切换对应演示账号。九个业务场景和六个 AI 样例入口直达可操作页面。

demo 环境预置两只宠物、三件商品、六条体重、两条日志、一笔待付款订单和一笔虚构租金账单，并保留原有健康记录、提醒与演示医院。修改立即生效，重启演示服务后恢复初始数据，不连接真实 MySQL。

![样例体验与三种角色入口](evidence/demo/showcase.png)

#### 样例操作结果

以宠物主人进入“生活日志”，编辑样例后保存，页面展示更新内容；成长趋势已有三次称重。普通用户与商户分别看到同一笔样例订单，可体验取消或状态处理。取消后可重新下单继续演示。

![样例日志编辑保存结果](evidence/demo/logs.png)

快捷入口仍使用 JWT、CSRF 与角色权限校验。`/api/demo/status` 仅在启用 demo profile 时返回虚构账号；其他环境返回 enabled=false。商户审核可配合注册页创建申请；AI 可一键填入样例图片、问题或宠物信息，再由用户提交生成结果，没有伪造分析历史。
'''
    block += '\n<!-- demos:end -->\n'
    marker = re.search(r'\n### (验证与总结反思|AI 安全与验证|关键边界与验证)', part)
    parts[i] = part[:marker.start()] + block + part[marker.start():] if marker else part + block
text = ''.join(parts)
counter = 0
def caption(match):
    global counter
    counter += 1
    label = re.sub(r'^图\d+\s*', '', match[1])
    return f'![图{counter} {label}]({match[2]})'
text = re.sub(r'!\[([^]]*)\]\(([^)]+)\)', caption, text)
source.write_text(text.rstrip() + '\n', encoding='utf-8')
print(f'Integrated 33 feature scenes, 2 sample scenes, {counter} total figures')
