---
feature: view-to-compose-dialogs
status: in-progress
updated: 2026-09-09
branch: liquid
commits: 
---

# View 布局收敛：死代码扫描 + 阅读内弹层 Compose 化

## Report

## [S1] Problem
阅读页仍残留大量 View 布局与 DialogFragment；部分布局已无宿主（死代码），P0 弹层（自动翻页、点击区域）仍走独立窗口，无法与玻璃底栏/真采样一致，且易出现 layout 写状态卡死。

## [S2] Design
1. **死代码扫描**：统计 `app/src/main/res/layout` 中无 Kotlin/XML 引用的布局，删除可证明无宿主者；保留仍被 inflate 的（如 `view_read_menu` 若仍被引用则先查宿主）。
2. **P0 阅读内弹层**：`AutoReadDialog`、`ClickActionConfigDialog` 改为 in-tree 玻璃浮层，复用 `LiquidGlassDialog` + `GlassConfig`；功能与原 Dialog 一一对应；不在 AndroidView layout 中写 snapshot state 或同步 `loadContent`。
3. **P1 模板**：抽出可复用的「玻璃底部/全屏面板」接入方式（状态驱动 + backdrop 注入），供后续 Dialog 批量迁移；本轮至少落地一个 P1 样例或明确模板文件。

契约：
- 打开路径：Activity 设置 `readPageState.showXxx` → `ReadBookScreen` 渲染 Compose 面板，采样 `readBackdrop`。
- 关闭：`showXxx=false`，`bottomDialogCount` 对称增减；必要时 `ReadBookConfig.save()` / `AppConfig.detectClickArea()`。
- 视觉：容器圆角/颜色走 `GlassConfig.sheetCornerRadius` / `containerColor(书页明暗)`；列表选择类若需要再对齐主题模式弹框。

## [S3] Out of Scope
- Canvas 翻页、漫画、音频播放器、WebView、代码编辑器（P2/P3 暂缓）。
- 全量 50+ Dialog 一次性迁移（P1 只做模板 + 可选样例）。
- 全局 Preference 基建 `view_preference*.xml` 重写。

## Tasks
- [ ] T1: 死布局扫描与删除 — acceptance: 输出无引用布局清单；确认删除项后 `assembleAppDebug` 通过 (covers: S2)
- [ ] T2: AutoRead 改玻璃浮层 — acceptance: 自动翻页弹层为 Compose/LiquidGlassDialog，速度调节/目录/菜单/停止/设置可用，不再使用 dialog_auto_read (covers: S2)
- [ ] T3: ClickActionConfig 改玻璃浮层 — acceptance: 九宫格点击区域可配置并写入 PreferKey，关闭后 detectClickArea，不再使用 dialog_click_action_config (covers: S2)
- [ ] T4: P1 迁移模板落地 — acceptance: 文档化可复用接入方式；若迁一个样例则 assembleAppDebug 通过 (covers: S2; depends: T2)
