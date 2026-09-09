# CHANGELOG

> 567Reader 由 legado-E（阅读 Sigma）fork 而来（包名 com.qreader.reader）。以下记录自 fork 后的实际改动，按日期提炼。

**2026/08/31**

* fork 自 legado-E，重命名为 567Reader；最低 API 提升至 33（Android 13）。
* 移除在线更新、RSS 导航 tab、日志功能、「关于」页面、本地密码、备份加密。
* 优化 Gradle 编译速度（并行构建、构建缓存、配置缓存、Kotlin 增量编译）。
* 集成 Liquid Glass 效果（kyant drawBackdrop API），删除书籍弹框替换为玻璃态。
* 升级构建链：Kotlin 2.4.10、AGP 9.3.2、Compose 2025.08、Liquid Glass 2.0.1、Gradle 9.7.1。

**2026/09/01**

* 升级 compileSdk 到 37；修复 AGP 9.0+ 兼容；版本号格式定为「主.子.阶段.日期_阶段」。
* 移植官方 LiquidBottomTabs 演示，抽出 GlassButton/GlassCard 组件。
* LiquidDialog 忠实移植官方 DialogContent；删除书籍弹框玻璃化。
* 「我的」设置页 Compose 化（Liquid Glass 分组卡片）。
* 主题模式弹框玻璃态化（DialogFragment + 截图作 backdrop）。
* 主界面整页迁移 Compose：底部导航栏改为 LiquidBottomTabs 玻璃态胶囊栏。
* 底部导航栏玻璃设置页（实时调参 + 保存即时生效）。
* 三 Fragment 迁移 Compose：HorizontalPager 替换 ViewPager，新增 BookshelfPage / ExplorePage / SettingsPage。

**2026/09/02**

* 修复主界面四个 UI 问题（状态栏 padding、滑动卡顿、书架首尾多余 margin）。
* 书架补回标题栏、还原分组样式；顶栏补回搜索与更多选项菜单。
* 设置页补回标题栏，三页标题栏统一。
* 修复点击底部导航栏跨页跳「卡在发现页」；修复滑动「段落感」（切断 selectedTab 双向回环）。
* 主题模式弹框改为同 surface 玻璃覆盖层（真毛玻璃），取代独立 DialogFragment。
* 导航栏 edge-to-edge（全 App 生效，透明手势条）；折射率 24 → 48。
* 标题栏玻璃态化：新增玻璃标题栏 overlay，恢复书架搜索与更多选项菜单。

**2026/09/03**

* 抽出通用玻璃弹框视觉令牌 GlassDialogTokens 供三处复用。
* 编辑分组弹框改为 Compose 玻璃态；抽出通用 LiquidGlassDialog 组件。
* 应用显示名改为「阅读567」；重写 README / CHANGELOG。
* 书架「更多选项」菜单玻璃态化（自绘 GlassDropdownMenu）。
* 修复主题弹框侧滑返回后标题栏与底部导航栏永久消失（LiquidGlassDialog 内置 BackHandler）。

**2026/09/04**

* 书架排序弹框玻璃态化（排序值提升到 MainScreen，弹框渲染在捕获层之外）。
* 新增分组抽屉功能（玻璃态分组按钮 + GroupDrawerOverlay 宫格）。
* 移除视频播放器模块及 GSYVideo / 弹幕库依赖。
* ContentHelp 性能优化：makeDict 字典从 ArrayList 改 HashSet。
* 清理 style1/style2 书架死代码（~1,300 行）；移除冗余菜单项。

**2026/09/05**

* 移除设置页「文件管理」入口。
* Web 服务开关改为玻璃态胶囊开关（自建 trackBackdrop 采样轨道内容）。
* 优化页面切换卡顿：下调 HorizontalPager beyondViewportPageCount。

**2026/09/06**

* LiquidToggle 完整移植官方实现（拖拽/点击手势、awaitPointerEvent 手动跟踪）。
* 修复设置页闪退（layerBackdrop 改为条件性启用）。
* 页面切换性能：程序化滚动期间暂停 layerBackdrop；点击切页统一「瞬跳 + 淡入」。
* 修复 LiquidToggle 点击时看不到滑块放大（DampedDragAnimation 新增 minHoldDuration）。
* 书架「更多」溢出菜单恢复「分组管理」入口。

**2026/09/07**

* 书籍信息界面迁移到 Compose（BookInfoScreen.kt + 最小桥接布局）。
* 书籍信息页标题栏玻璃态化；下拉刷新改为 PullToRefreshBox。
* 删除书籍弹框改为 Compose 玻璃态；移除分享按钮。
* 删除 RSS UI 层（Activity / Fragment / ViewModel / Adapter / Dialog）。
* 搜索页 Compose 化（SearchActivity 混合模式）；按 legado-E 还原标题栏。
* 搜索逻辑忠实还原 legado：已知书架搜索、空结果弹窗、搜索范围变更自动重搜。
* 搜索页进度条样式统一（顶部 2dp RefreshProgressBar）。

**2026/09/08**

* 搜索页标题栏玻璃态化并对齐发现页；样式按 legado-E 源码还原。
* 修复搜索页文字在暗色下看不清（内容层加背景 + 资源色配色）。
* 书籍信息页模糊封面背景改为全屏方案（fillMaxSize + CenterCrop + 0x50000000 遮罩）。
* 搜索页回归 legado 忠实行为：回退「惰性预抓详情补全分类」。
* 隐藏孤零零的字数标签（kind 为空时不渲染 LabelsBar）。
* 修复单书源搜索「停止/开始死循环」（自动加载条件加 isScrollInProgress 闸门）。
* 修复书籍信息页标题栏丢失「编辑」与「更多选项」按钮（抽出手 handleMenuAction，GlassDropdownMenu 提取为共享组件）。
* 修复发现页文字乱码（MainScreen.kt 编码损坏，用干净版本重建）。
* 发现页分组菜单玻璃态化（与书架页共用 GlassDropdownMenu）。
* 分组弹框图标改为组内前 4 本封面的 2×2 四格拼图。
* 玻璃配色收敛为单一真源（GlassConfig.isLightTheme 按背景色判定）。
* 修复书籍信息页「内容少时底部露出模糊封面」（BoxWithConstraints + onSizeChanged 实测高度补底）。
* 玻璃态参数统一收敛到 GlassConfig 全局配置（glassButtonSurfaceAlpha / glassButtonCornerRadius / titleBarHeight / pseudoGlass 修饰符）。
* 书籍信息编辑页 Compose 化 + 玻璃标题栏。
* 删除 GlassDemoActivity（死代码）。

**2026/09/09**

* 欢迎页迁到 Compose 并玻璃化（氛围底 + pseudoGlass 品牌卡）；跳转改主线程 Handler，避免嵌套 drawBackdrop 卡死。
* 删除 17 个无引用 View 布局（旧书籍信息、style2 书架 item 等，~57KB）。
* 书架列表改为 Compose LazyColumn / LazyVerticalGrid；先试玻璃卡片后回退为无折射简洁列表。
* GlassConfig 对齐官方 AndroidLiquidGlass：blur 12→8dp，lensY 48→24dp，亮色容器 alpha 0.6→0.4。
* 阅读页设置改为玻璃底部面板；设置项行距收紧；LiquidToggle 整轨可点可拖。
* 双页等列表选择弹框玻璃态化；主题模式/列表弹框改为选中即生效。
* 界面面板改为 in-tree 玻璃底栏；界面滑杆玻璃态（后回退内嵌方案，避免整页卡死）。
* 固定部分阅读配置（刘海、状态栏、屏幕方向/超时等）并删除对应设置入口。
