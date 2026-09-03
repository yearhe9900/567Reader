# 567Reader 项目长期记忆

## 硬性禁止（git 操作）
- **绝对禁止 agent 执行 `git rm` 与 `git mv`**。本仓库曾两次因此搞坏工作树（369 文件 "deleted"、`ui/main/` 目录消失；stale `.git/index.lock` 阻塞后续 git 静默失败）。
- 重命名/删除的正确做法：**普通 `rm` / `mv` 改文件系统** → `git add -A`（让 git 自行 reconcile 重命名/删除）→ **单次 `git commit`**。
- 遇 `fatal: Unable to create '.git/index.lock'`：先 `rm -f .git/index.lock`，再 `git checkout HEAD -- .` 恢复，然后重跑上面的安全流程。

## Git 工作流
- agent 每次改动后自动 `git add` + `git commit`（用户明确要求「每次改完提交 git」）。
- 行尾：仓库 Windows + autocrlf，工具写 LF、checkout 归一为 CRLF，会报 "LF will be replaced by CRLF"。改完提交后 `git checkout -- <file>` 把工作区 renormalize 回 CRLF 使 status 干净。
- build / adb 命令**由用户手动跑**，agent 不执行（用户明确要求）。
- `.workbuddy/artifacts/` 的临时产物（如 overview.md）不应进版本库，勿 `git add -A` 时误带。

## 架构关键事实
- 玻璃态是 Compose-only（kyant `drawBackdrop` / `layerBackdrop`）：overlay 必须是 backdrop 捕获的「兄弟节点」才能采样真实页。编辑分组/主题弹框均用全屏外层 Box 直接子节点的 `AnimatedVisibility` + `LiquidGlassDialog`。
- 「蒙板不全屏」优先查：外层容器是否全屏、弹框是否被塞进 align/fillMaxWidth 的小 Box（如底部导航栏）。
- **in-tree overlay 必须自己挂 `BackHandler`**：它不是 Dialog，返回键/侧滑返回不会自动被它吃掉，会穿透到 `MainActivity.onBackPressedDispatcher`（其首条逻辑是 `selectedTab != 0 → 切回书架`）。`LiquidGlassDialog` 已内置；判空用 `LocalOnBackPressedDispatcherOwner.current != null`（DialogFragment 内可能为 null，直接调 BackHandler 会抛异常）。
- 弹框开关状态若由比宿主页面更上层持有（如 `themeDialogOpen` 在 MainActivity、弹框在 Pager 的设置页里），宿主页被 Pager 销毁时**状态不会复位**，会出现「UI 没了但状态还开着」的幽灵状态（曾导致标题栏/导航栏永久隐藏）。此类状态要么随宿主销毁复位，要么加兜底复位副作用。
- 分组文件夹宫格仅在 Folder 样式（`AppConfig.bookGroupStyle == 1`）下渲染；Tab 样式不显示分组卡片。

## 目录约定
- 实验/副本页放 `app/src/main/java/com/qreader/reader/ui/main/`。
