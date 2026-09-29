# Git 协作与 CI 实验

本地改造在 `feature/personal-training` 分支进行。首次基线提交记录原系统代码及已确认方案；提交前已将数据库密码和 JWT 秘密配置改为环境变量，并排除运行上传目录、依赖及构建输出。

## 日常协作

```sh
git switch -c feature/my-change
git add <本次修改的文件>
git commit -m "feat: describe the behavior change"
git push -u origin feature/my-change
```

在用户指定的远程仓库创建 Pull Request，按仓库模板填写问题、改动和实际验证。AI 先检查潜在缺陷及摘要；最终由人工审查代码、证据和数据迁移后合并。当前工程如未配置 origin，则远程 PR 与云端 CI 尚未执行。

## 可复现的冲突练习

在独立临时练习仓库运行以下步骤，避免污染主工程：

```sh
git init -b main
git config user.name "Your Name"
git config user.email "your-email@example.com"
echo base > exercise.txt
git add exercise.txt
git commit -m "exercise: base"
git switch -c feature/a
echo version-a > exercise.txt
git commit -am "exercise: a"
git switch main
git switch -c feature/b
echo version-b > exercise.txt
git commit -am "exercise: b"
git merge feature/a
```

最后一步应出现内容冲突。打开 exercise.txt，根据双方需求保留正确内容并移除冲突标记，再执行 `git add exercise.txt` 和 `git commit`。报告应放实际冲突和处理截图，不把这些指令本身当作已完成协作证据。

## 持续集成

`.github/workflows/ci.yml` 在 main、feature 分支 push 和 PR 时分别执行 Java 17 后端测试打包与 Node 22 前端测试构建。两条任务均成功才可满足基础 CI 验收。测试报告以 artifact 保存；本地命令成功不等同于云端工作流已经运行。
