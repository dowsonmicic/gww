# Git 仓库使用说明

## 仓库与当前进度

| 项目 | 信息 |
| --- | --- |
| 本地仓库 | `D:\work\gww` |
| 远程仓库 | https://github.com/dowsonmicic/gww |
| 远程名称 | `origin` |
| 远程地址 | `https://github.com/dowsonmicic/gww.git` |
| 当前分支 | `A1` |
| 跟踪分支 | `origin/A1` |
| 一期A步初始提交 | `8b15e25` |

`8b15e25` 保存一期A步中英文展示官网、提供的图片、自动化测试、README 和分步部署方案。

以下命令在项目根目录 `D:\work\gww` 执行。当前项目已经初始化 Git，无需再次运行 `git init` 或添加 `origin`。

## 查看状态

查看当前分支和未提交文件：

```powershell
git status
```

查看尚未暂存的修改：

```powershell
git diff
```

查看已经暂存、准备提交的修改：

```powershell
git diff --cached
```

查看最近的提交记录：

```powershell
git log --oneline -10
```

查看本地分支及其远程跟踪关系：

```powershell
git branch -vv
```

## 保存并上传修改

先检查修改，再按实际文件名暂存。以下以 Git 文档为例，不会自动包含其他文件：

```powershell
git add -- docs/plan/GitREADME.md
```

提交前检查内容和空白问题：

```powershell
git diff --cached
git diff --cached --check
```

创建一次新提交，使用 PowerShell 多行字符串传递说明：

```powershell
$message = @'
docs: 补充项目Git使用说明

记录A1进度及日常提交、推送和分支操作。
'@
git commit -m $message
```

上传当前 `A1` 分支：

```powershell
git push origin A1
```

只有提交成功后才推送。提交保存到本地历史，推送才会上传 GitHub。

## 获取远程更新

下载远程分支信息，但不修改当前工作文件：

```powershell
git fetch origin
```

确认工作区干净后，仅允许快进更新当前分支：

```powershell
git pull --ff-only
```

如果提示分支已经分叉，不强制覆盖，也不要直接重置；先检查双方提交，再决定合并方式。

## 后续创建分支

建议分支用途如下，除 `A1` 外均为计划名称，并未创建：

| 分支 | 建议用途 |
| --- | --- |
| `A1` | 一期A步完成记录 |
| `A2` | A步后续页面调整 |
| `B1` | 一期B步接口、数据库和真实询盘 |
| `C1` | 一期C步登录后台和服务器本地媒体 |
| `D1` | 一期D步上线及越南访问验收 |

例如从已确认的 `A1` 创建 `B1`，先保存当前修改并检查状态，再执行：

```powershell
git switch A1
git pull --ff-only
git switch -c B1
```

首次上传新分支并设置远程跟踪：

```powershell
git push -u origin B1
```

后续在 `B1` 上提交后可直接使用 `git push`。分支名仅是示例，不要在未开始对应阶段时批量创建。

## 在另一台电脑下载

在准备存放项目的父目录执行以下命令，下载 `A1` 分支：

```powershell
git clone --branch A1 https://github.com/dowsonmicic/gww.git
```

下载后按 [项目README](../../README.md) 安装 Node 24 LTS 和前端依赖。Git 仓库不包含 `node_modules` 或生产构建结果。

## 不应提交的内容

当前 `website/.gitignore` 排除了以下内容：

- `node_modules/`、`.nuxt/`、`.output/`
- `website/.env`
- 测试报告目录和日志

该忽略文件仅作用于 `website` 下，根目录 `.env` 或其他环境配置不一定被忽略。所有 `.env`、密钥、数据库密码、OSS AK/SK、证书私钥和客户询盘数据都不应提交，即使 Git 将它们显示为未跟踪文件。

提交时优先指定文件，不直接使用 `git add .`，避免意外包含敏感配置或无关文件。源码、锁文件、授权图片和方案文档可以提交。

## 注意事项

- 切换分支前先检查并妥善保存当前修改。
- 不使用强制推送覆盖远程历史。
- 不直接使用 `reset --hard`、`clean -f` 丢弃未保存工作。
- 默认创建新提交，不修改已上传提交。
- 若凭证已经上传，仅删除文件不能消除历史泄露；应立即撤销或轮换凭证，再处理历史。
- `A1` 分支可能继续增加提交；固定的A步初始快照以提交 `8b15e25` 为准。

本文件仅提供操作说明，不会自动执行提交、推送或创建后续分支。
