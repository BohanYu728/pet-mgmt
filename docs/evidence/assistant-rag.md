# 项目资料问答验证

接口：`POST /api/assistant/ask`，请求体 `{ "question": "疫苗提醒如何使用？" }`。成功响应包含 `answer`、`sources`（每项有 `id`、`title`、`excerpt`）和 `status`。状态分别为 `answered`（远程模型生成且引用ID经服务端核对）、`retrieval_only`（返回资料原文）、`no_sources`（未找到相关资料）。无效问题返回 HTTP 400。

知识库位于 `src/main/resources/knowledge/project-guide.md`，依据项目内《宠物管家系统设计与实现》整理。按二级标题切分，每片段至多450字符，引用 ID 为文件名加章节名。第一次配置模型后，服务端使用嵌入接口批量生成知识向量并缓存；每个问题调用同一嵌入模型，按余弦相似度选取至多3段。未配置密钥或远程服务出错时，仅返回匹配的资料原文，并标明 `retrieval_only`，不会冒充模型回答。

配置只在服务端读取：`OPENAI_API_KEY`、`OPENAI_BASE_URL`、`OPENAI_CHAT_MODEL`、`OPENAI_EMBEDDING_MODEL`。默认模型分别为 `gpt-4.1-mini` 和 `text-embedding-3-small`。依赖为官方 [OpenAI Java SDK](https://github.com/openai/openai-java) 4.68.0。配置密钥后才能验证真实远程推理；本地测试未发送付费 API 请求。

SDK 每次请求设置20秒超时和零次重试，避免服务长时间挂起或自动重复计费。嵌入响应按 `index` 还原原始输入顺序；数量不符、重复索引、越界索引和无效向量均作为失败处理。`OpenAiGatewayProtocolTest` 在本机启动假 HTTP 服务，实际通过 SDK 发送嵌入与聊天请求，验证请求模型、鉴权头、system/user 角色、乱序向量恢复以及可核对的引用。它不调用外部 API。

服务端检查输入长度、控制字符、嵌入结果维度、答案长度及所有引用 ID。资料和用户文本均作为不可信输入，系统消息要求只按检索资料回答。引用校验仅保证 ID 来自本次检索资料，不会证明每句话的语义真实性，因此健康回答仍提示咨询兽医。

验证命令：`mvn.cmd -q '-Dmaven.repo.local=.m2repo' '-Dtest=AssistantServiceTest,OpenAiGatewayProtocolTest' test`。测试覆盖无密钥、远程失败、向量检索、知识切分、无效输入、伪造引用和 SDK 协议。
