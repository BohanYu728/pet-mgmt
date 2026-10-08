// Inputs only. Every result is produced by the existing authenticated API.
export const aiSamples = {
  assistant: { title: '疫苗提醒怎么设置', description: '填入常见照护问题，查看回答、参考出处与实际运行状态。', question: '如何设置疫苗提醒？', boundary: '未配置大模型时展示资料检索结果，状态为 retrieval_only。' },
  breed: { title: '边牧照片识别', description: '加载一张边牧示例照片，可更换为自己的图片后提交。', image: '/images/border-collie-sample.jpg', species: 'dog', boundary: '使用本地 MobileNetV2 图片分类模型，分数不是识别准确率。照片用于演示，不代表档案宠物本人。' },
  emotion: { title: '猫咪照片情绪分析', description: '用猫咪样例照片体验分析和历史记录，提交后查看本次结果。', image: '/images/cat-lifestyle.jpg', species: 'cat', boundary: '使用本地 CLIP 比较图片与状态描述，支持猫狗；相似度不是情绪概率，结果仅为推测。' },
  health: { title: '旺财的照护报告', description: '选择样例狗狗，使用它的档案、健康与照护记录生成参考报告。', species: 'dog', boundary: '无外部模型时使用本地算法，结果仅供照护参考，不是诊断。' },
  recommend: { title: '为旺财挑选生活用品', description: '选择样例狗狗，根据宠物资料和在售商品生成推荐。', species: 'dog', boundary: '样例商品均为虚构数据；未配置外部模型时使用本地推荐算法。' },
  consultation: { title: '食欲变化咨询', description: '填入一段虚构的观察记录，修改种类、症状和持续时间后再提交。', species: 'dog', symptoms: '虚构样例：狗狗今天食欲比平时减少，饮水正常，活动略少，没有呕吐。希望了解应该观察和记录哪些变化。', duration: '一天（虚构样例）', boundary: '仅供功能体验；症状与建议不能代替兽医检查，紧急情况请及时就医。' }
}
