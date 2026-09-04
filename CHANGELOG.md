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
* 标题栏玻璃态化：新增玻璃标题栏 overlay（与底部导航栏共享 backdrop）；玻璃标题栏内恢复书架搜索（玻璃按钮）与更多选项菜单（DropdownMenu 11 项，还原 R.menu.main_bookshelf）。

**2026/09/03**

* 抽出通用玻璃弹框视觉令牌 GlassDialogTokens 供三处复用（深色固定配色 + E-Ink 回退）。
* 三页默认进入 top padding 105dp → 110dp；书架下拉刷新圆环 top offset 110dp（避开玻璃标题栏）。
* 玻璃态标题栏编译修复：registerMenuAction 类型括号不匹配、handleMenuAction 前向引用、清理未用导入；新增 .gitattributes 统一文本行尾为 CRLF 消除告警。
* 编辑分组弹框改为 Compose 玻璃态（仅书架长按入口，对标主题弹框、其余 XML 入口保持原样）。
* 抽出通用 LiquidGlassDialog 组件；多次修复编辑分组蒙板不全屏——误判 AnimatedVisibility 的 wrapContentSize，真根因为 AnimatedVisibility 被嵌套进底部导航栏小 Box，移出为全屏外层 Box 直接子节点解决。
* 修复编辑分组弹框「分组名称」label 黑色看不清（给 BasicText 显式上 contentColor）。
* BookshelfPageNew 转正为正式 BookshelfPage，删除旧兜底页（迁移 BookshelfMenuAction 枚举，避免 Unresolved reference）。
* 清理误入库的 .workbuddy/artifacts/overview.md 并加入 .gitignore。
* 重写 README.md 反映 567Reader 真实项目。
* 应用显示名改为「阅读567」（覆盖全部多语言 app_name 及 _a/_s 变体）；重写 CHANGELOG.md 按天提炼 fork 后改动。
* 书架「更多选项」菜单玻璃态化：Material3 DropdownMenu 走独立 Popup 窗口、无法被 drawBackdrop 采样，改为自绘 GlassDropdownMenu（全屏点击关闭层 + 顶部右对齐玻璃面板，距顶 108dp 避开标题栏）；菜单状态提升至 MainScreen，渲染在全屏外层 Box 内且位于标题栏之下（避免点击关闭层吞掉「更多」按钮点击）。菜单项图标 24dp、文字 16sp 对齐原版 PopupMenu，宽度固定 200dp。
* 移除书架菜单中的「日志」项（菜单由 11 项 → 10 项）。
* 修复：主题模式弹框打开时不点按钮、侧滑返回退出，标题栏与底部导航栏永久消失。根因为 in-tree overlay 不是 Dialog、返回事件穿透到 MainActivity 把 tab 切回书架，设置页被 Pager 销毁而 themeDialogOpen 状态未复位。改为 LiquidGlassDialog 内置 BackHandler 接管返回键与侧滑手势（三处玻璃弹框统一生效，DialogFragment 场景判空跳过），并在 MainScreen 增加翻页兜底复位开关。

**2026/09/04**

* 书架排序弹框玻璃态化（经多轮定位根因）：弹框在 BookshelfPage 内部（HorizontalPager 内、MainScreen 的 layerBackdrop 捕获层内部）创建本地 backdrop → drawBackdrop 采样到包含弹框自身的层 → 循环捕获崩溃。最终照搬 GroupEditOverlay 模式——排序值 bookshelfSort 提升到 MainScreen，新增 SortDialogOverlay 渲染在全屏外层 Box 直接子节点、layerBackdrop 捕获层之外，复用 MainScreen 的 backdrop。
* 排序弹框视觉升级：宽度 0.78f、圆角 48dp（对齐主题弹框），TextButton 行改为 clickable Row + drawBackdrop 玻璃态单选圈（Capsule），选中项 accentColor 填充 + 8dp 白色小圆点，即点即用无确认按钮。
* 移除「添加网址」菜单项及相关代码（BookshelfViewModel.addBookByUrl、AlertDialog、ic_add_online drawable），净减 151 行。
* 布局切换菜单项动态显示「列表布局/网格布局」（titleRes 改 var，LaunchedEffect 和 handleMenuAction 同步更新）。
* 新增分组抽屉功能：标题栏搜索按钮旁新增玻璃态分组按钮（ic_groups），点击打开 GroupDrawerOverlay（LiquidGlassDialog 内 3 列文件夹宫格，ic_folder / ic_folder_open 图标，选中项高亮）。点击分组切换书架数据源（currentGroupId 驱动 flowByGroup），标题栏动态显示当前分组名。
* 菜单按钮改为 toggle（新增 menuOpen 参数，再次点击可关闭菜单）。
* 移除视频播放器模块（ui/video、help/gsyVideo、VideoPlayService、model/VideoPlay）及 GSYVideo / 弹幕库依赖。
* ContentHelp 性能优化：makeDict 字典从 ArrayList 改 HashSet，contains 从 O(m) 降到 O(1)。
* 清理 style1/style2 书架死代码（~1,300 行）；移除缓存/导出、分组管理、导出书单、导入书单菜单项；移除冗余的「更新目录」菜单（下拉刷新已覆盖）。
