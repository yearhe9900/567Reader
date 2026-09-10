---
feature: view-to-compose-dialogs
status: delivered
updated: 2026-09-09
branch: liquid
commits: ad18fd395d56ff415c753d3c58ca507623a153a6..80b86de
---

# View 布局收敛：死代码扫描 + 阅读内弹层 Compose 化

## Report

**What was built** — 在 `liquid` 上完成 compose-next 第 1–3 阶段：删除无宿主的 `ReadMenu.kt` 与自动翻页/点击区域 Dialog；新增 `AutoReadGlassSheet`（液态速度条 + 目录/菜单/停止/翻页动画）与 `ClickActionGlassSheet`（3×3 点击区域，写 PreferKey，关闭时 `detectClickArea`）；二者均为 `LiquidGlassDialog` + `showScrim=false` 真采样正文。`view_read_menu.xml` 因 `MangaMenu` 依赖 `R.id.ll_brightness` 保留。P1 迁移模板即本模式：`ReadPageOverlayState.showXxx` + `ReadBookScreen` 宿主 + `GlassConfig` 尺寸/配色，已由 MoreConfig/ReadStyle/AutoRead/ClickAction 四处落地。

**Verification** — `.\gradlew.bat :app:assembleAppDebug` 于 a36304c 与 80b86de 均为 BUILD SUCCESSFUL。独立 review（general-1）对 a36304c：Spec PASS、一致性 PASS、Correctness FAIL（autoPageStop 无条件减 count、停止双重减、打开未 pause 翻页）；已在 80b86de 修复三项关键点并再次 assembleAppDebug 通过。

**Journey log**
- 1. 布局死代码扫描须同时匹配 `R.layout.x` 与 `XxxBinding`；PowerShell `-join '' + 'Binding'` 会变成分隔符，需括号包 join。
- 2. AndroidView + Compose `mutableStateOf` / `RadioGroup.check`→`loadContent` 会整页卡死（先前会话），故 P0 坚持纯 Compose 浮层。
- 3. `bottomDialogCount` 必须与面板真实显隐对称；`autoPageStop` 这类全局路径不得无条件减计数。
- 4. `view_read_menu.xml` 虽无 ReadMenu 类，仍供 `R.id.ll_brightness`，删类可、删布局暂不可。

## [S1] Problem
阅读页残留大量 View 布局；P0 弹层走独立 Dialog 窗口，无法与玻璃底栏真采样一致。

## [S2] Design
1. 删除可证明无宿主的 View 代码；保留仍提供 R.id 的布局。
2. AutoRead / ClickAction → in-tree `LiquidGlassDialog`，功能对齐原 Dialog；打开/关闭与 `bottomDialogCount`、`onMenuShow`/`onMenuHide` 对称。
3. P1 模板：状态驱动 + backdrop + GlassConfig，见已交付四份 GlassSheet。

契约：
- 打开：`readPageState.showXxx=true` + `bottomDialogCount++`（必要时 `onMenuShow`）。
- 关闭：`showXxx=false` + `count--`；仅在面板曾打开时由业务路径关闭。
- 视觉：`GlassConfig.sheetCornerRadius` / `containerColor(书页明暗)`，`showScrim=false`。

## [S3] Out of Scope
- Canvas/漫画/音频/WebView/编辑器（P2/P3）。
- 全量 50+ Dialog 批量迁移。
- `view_preference*.xml` 全局重写。

## Tasks
- [x] T1: 死布局扫描与删除 — acceptance: ReadMenu.kt 删除；view_read_menu 保留；assembleAppDebug 通过 (covers: S2)
- [x] T2: AutoRead 改玻璃浮层 — acceptance: AutoReadGlassSheet 替代 AutoReadDialog；速度/目录/菜单/停止/设置可用 (covers: S2)
- [x] T3: ClickAction 改玻璃浮层 — acceptance: 九宫格写 PreferKey；关闭 detectClickArea (covers: S2)
- [x] T4: P1 迁移模板落地 — acceptance: 模式已在 4 个 GlassSheet 固化并写入本文档 (covers: S2; depends: T2)
