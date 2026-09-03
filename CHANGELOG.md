# CHANGELOG

> 567Reader 由 legado-E（阅读 Sigma）fork 而来（包名 com.qreader.reader）。以下记录自 fork 后的实际改动，按日期提炼。

**2026/08/31**

* fork 自 legado-E，重命名为 567Reader；固定 APK 输出名为 567Reader.apk；最低 API 提升至 33（Android 13）。
* 移除在线检测更新功能（help/update 整个目录、UpdateDialog、自动更新相关偏好与多语言字符串）。
* 移除 RSS 订阅导航 tab（底部 4 tab → 3 tab，保留 RSS 数据层与导入导出）。
* 移除日志功能（UI 入口、CrashLogsDialog、AppLogDialog 等改空 stub）；移除「关于」页面入口、本地密码、备份加密。
* 优化 Gradle 编译速度：并行构建、构建缓存、配置缓存、Kotlin 增量编译。
* 集成 Liquid Glass 效果（kyant `drawBackdrop` API），演示页加入液态玻璃底部导航栏；删除书籍弹框替换为玻璃态效果。
* 升级构建链：Kotlin 2.4.10、AGP 9.3.2、Compose 2025.08、Liquid Glass 2.0.1、Gradle 9.7.1。

**2026/09/01**

* 升级 compileSdk 到 37；修复 AGP 9.0+ 兼容（改用 androidComponents API、移除 kotlin.android 插件）；修正 KSP 版本兼容 Kotlin 2.4.10。
* 版本号格式定为「主.子.阶段.日期_阶段」（如 1.0.0.260901_release）；「我的」页新增系统版本显示。
* 移植官方 LiquidBottomTabs 演示，抽出可复用 GlassButton/GlassCard 组件。
* LiquidDialog 忠实移植官方 DialogContent；删除书籍弹框玻璃化（修复复选框对齐、背景被压暗无光圈、弹框「像换页」等问题，改为截原页 bitmap 作玻璃 backdrop）。
* 「我的」设置页 Compose 化（Liquid Glass 分组卡片），经多轮微调定稿为纯色背景 + 大圆角分组卡片；标题栏按参考图反复压顶间距。
* 主题模式弹框玻璃态化：绕过 Compose Dialog/Popup 触发父 Activity 下移/闪动/系统栏变色的坑，最终采用 DialogFragment（`STYLE_NO_FRAME` + 透明主题 + 调用方截当前页作 backdrop）+ `onStart` 固定系统栏图标配色，与删除弹框同一机制。
* 主界面整页迁移 Compose：底部导航栏替换为 LiquidBottomTabs 玻璃态胶囊栏（内容区 ViewPager 暂保留 View 体系）；导航栏改悬浮 overlay（内容可透到导航栏背后）。
* 底部导航栏玻璃设置页（实时调参 + 保存即时生效）；修复 Activity 字段初始化器访问 SharedPreferences 导致的启动闪退；修复滚动列表底部被悬浮栏遮挡（固定底部 72dp padding）。
* 玻璃参数对齐官方 GlassPlayground（blur / refractionHeight / refractionAmount / chromaticAberration）；修复导航栏「无折射」——新增隐藏渐变层作 backdrop 采样源。
* 三 Fragment 迁移 Compose：HorizontalPager 替换 ViewPager，新增 BookshelfPage / ExplorePage / SettingsPage（RecyclerView 经 AndroidView 桥接，逻辑不改）。

**2026/09/02**

* 修复主界面四个 UI 问题（状态栏 padding、滑动卡顿、书架首尾多余 margin）。
* 书架补回标题栏、还原分组样式（Tab=顶部分组 Tab / Folder=列表首项，对齐 legado-E 行为）；顶栏补回搜索与更多选项溢出菜单（还原 R.menu.main_bookshelf 11 项）。
* 设置页补回标题栏，三页标题栏统一；标题栏背景色经 statusBarColor → 纯黑/白 → 跟随主题背景色（backgroundColor）多轮迭代，最终定为全局固定值方案。
* 点击底部导航栏跨页跳「卡在发现页」根因定位（selectedTab 回环误触发 animateScrollToPage）并修复（相邻页 tween、跨页瞬跳+淡入）。
* 滑动「段落感」根因定位（导航栏 ↔ pager 的 selectedTab 双向回环），彻底切断回环；滑动丢帧三层根因（拖拽手势与程序滚动抢控制权、3 层玻璃 GPU 后处理、滑块 spring 拖长重绘）逐项修复。
* 主题模式弹框改为同 surface 玻璃覆盖层（真·毛玻璃，采样真实设置页、无截图），取代独立 DialogFragment；统一删除弹框与主题弹框玻璃态深色风格。
* 主题弹框打开时隐藏底部导航栏（状态提升至 MainActivity）；玻璃 Dialog 固定浅色风格且 E-Ink 模式降级为纯白高对比。
* 导航栏折射率默认值 24 → 32 → 48；主题弹框样式统一为深色套；导航栏折射真实化（backdrop 改挂到包裹 pager 的层，采样真实页面内容）。
* 导航栏 edge-to-edge（全 App 生效，透明手势条）；对齐 AndroidLiquidGlass demo 效果（24/24 折射、三层玻璃常开）。
* 标题栏玻璃态化尝试：新增玻璃标题栏 overlay，暂丢失搜索/菜单/分组 Tab 入口；随后在玻璃标题栏恢复书架搜索（玻璃按钮）+ 更多选项（DropdownMenu 11 项）。

**2026/09/03**

* 抽出通用玻璃弹框视觉令牌 GlassDialogTokens 供三处复用（深色固定配色 + E-Ink 回退）。
* 三页默认进入 top padding 105dp → 110dp；书架下拉刷新圆环 top offset 110dp（避开玻璃标题栏）。
* 玻璃态标题栏编译修复：registerMenuAction 类型括号不匹配、handleMenuAction 前向引用、清理未用导入；新增 .gitattributes 统一文本行尾为 CRLF 消除告警。
* 书架分组文件夹封面：四格封面 + 横向滑动进组，经 3×3 九宫格 → 2×2 四格反复、按分组自身排序取前 4 本；定位到「只在 Folder 样式下渲染」后整页回滚封面功能（书架回到干净基线）。
* 书架页副本 BookshelfPageNew 作实验沙箱（MainActivity 切到新页面，旧页留作兜底）。
* 编辑分组弹框改为 Compose 玻璃态（仅书架长按入口，对标主题弹框、其余 XML 入口保持原样）。
* 抽出通用 LiquidGlassDialog 组件；多次修复编辑分组蒙板不全屏——误判 AnimatedVisibility 的 wrapContentSize，真根因为 AnimatedVisibility 被嵌套进底部导航栏小 Box，移出为全屏外层 Box 直接子节点解决。
* 修复编辑分组弹框「分组名称」label 黑色看不清（给 BasicText 显式上 contentColor）。
* BookshelfPageNew 转正为正式 BookshelfPage，删除旧兜底页（迁移 BookshelfMenuAction 枚举，避免 Unresolved reference）。
* 清理误入库的 .workbuddy/artifacts/overview.md 并加入 .gitignore；固化「禁止 git rm / git mv，重命名删除改用 rm/mv + git add -A」硬规则（记忆）。
* 重写 README.md 反映 567Reader 真实项目。
