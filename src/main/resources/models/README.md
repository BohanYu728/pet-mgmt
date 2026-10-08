# 本地宠物图片分类

MobileNetV2 / ImageNet，ONNX opset 7，模型约 14 MB。模型来自 ONNX Model Zoo（Apache-2.0）：
https://github.com/onnx/models/tree/main/validated/vision/classification/mobilenet

类别表：https://github.com/pytorch/hub/blob/master/imagenet_classes.txt

输入采用 RGB、短边缩放至 256、中心裁剪 224、ImageNet 均值和标准差归一化。输出 softmax 模型分数；犬类索引为 151–268，家猫索引为 281–285。只有最高类别属于猫狗且对应类别分数总和达到 0.6 才返回候选品种，否则返回无法确认。模型分数不是实测准确率；其他动物、混种、遮挡、背景均可能影响预测。

运行时无需网络和 API Key。情绪分析使用 CLIP 本地图文相似度模型比较六类状态描述，支持猫狗。候选相似度未经情绪准确率校准，不能据单张照片确认真实情绪。损坏图片返回错误，不创建识别历史。
